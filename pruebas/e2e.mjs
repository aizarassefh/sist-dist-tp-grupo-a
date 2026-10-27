// Prueba de punta a punta contra los procesos reales: backend (REST/GraphQL +
// gRPC), motor NestJS y gateway. A diferencia de los tests de cada proyecto,
// que usan servidores gRPC en memoria, esta demuestra que el gateway en Java
// habla por la red con el backend Java y con el motor en TypeScript.
//
// Uso (con todo levantado): node pruebas/e2e.mjs
// Variables: GATEWAY_URL (http://localhost:8080), BACKEND_URL
// (http://localhost:8000), MUSEO_CLAVE (por defecto, la clave de los usuarios
// de ejemplo que figura en el README).
import { readFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const GATEWAY = process.env.GATEWAY_URL ?? 'http://localhost:8080'
const BACKEND = process.env.BACKEND_URL ?? 'http://localhost:8000'

// Se lee del README para no repetir la clave en otro archivo más.
function claveDeEjemplo() {
  if (process.env.MUSEO_CLAVE) return process.env.MUSEO_CLAVE
  const readme = readFileSync(join(dirname(fileURLToPath(import.meta.url)), '..', 'README.md'), 'utf8')
  const encontrada = readme.match(/Contraseña para todos\*\*: `([^`]+)`/)
  if (!encontrada) throw new Error('No encontré la clave de ejemplo en el README; usá MUSEO_CLAVE')
  return encontrada[1]
}

const resultados = []
async function prueba(nombre, fn) {
  try {
    const detalle = await fn()
    resultados.push({ nombre, ok: true, detalle: detalle ?? '' })
  } catch (error) {
    resultados.push({ nombre, ok: false, detalle: error.message })
  }
}
function exigir(condicion, mensaje) {
  if (!condicion) throw new Error(mensaje)
}

async function pedir(base, ruta, { metodo = 'GET', token, cuerpo } = {}) {
  const respuesta = await fetch(base + ruta, {
    method: metodo,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(cuerpo ? { 'Content-Type': 'application/json' } : {}),
    },
    body: cuerpo ? JSON.stringify(cuerpo) : undefined,
  })
  const texto = await respuesta.text()
  let json = null
  try { json = texto ? JSON.parse(texto) : null } catch { /* cuerpo no JSON (Excel) */ }
  return { estado: respuesta.status, json, tipo: respuesta.headers.get('content-type') ?? '' }
}

const graphql = (token, query) => pedir(BACKEND, '/graphql', { metodo: 'POST', token, cuerpo: { query } })
const serviciosDe = (json) => (json?.trazas ?? []).map((t) => t.servicio)
const trazasLegibles = (json) => (json?.trazas ?? []).map((t) => `${t.servicio}.${t.metodo} ${t.ms}ms`).join(' → ')

async function login(email) {
  const r = await pedir(BACKEND, '/api/auth/login', { metodo: 'POST', cuerpo: { email, password: claveDeEjemplo() } })
  exigir(r.estado === 200, `login de ${email} devolvió ${r.estado}`)
  return r.json.accessToken ?? r.json.token
}

const visitante = await login('visitante@test.com')
const curador = await login('curador@test.com')

// ---------- Entrega 1: tiene que seguir igual ----------
await prueba('E1 · catálogo GraphQL', async () => {
  const r = await graphql(visitante, '{ obras(tamanio: 3) { id titulo artista { nombre } } }')
  exigir(r.estado === 200 && r.json.data?.obras?.length > 0, `estado ${r.estado}`)
  return `${r.json.data.obras.length} obras`
})
await prueba('E1 · eventos REST', async () => {
  const r = await pedir(BACKEND, '/api/eventos', { token: visitante })
  exigir(r.estado === 200, `estado ${r.estado}`)
})
await prueba('E1 · reporte GraphQL (curador)', async () => {
  const r = await graphql(curador, '{ reporteAsistencia(filtro: { agruparPor: TIPO }) { grupos { tipo cantidadDeEventos } } }')
  exigir(r.estado === 200 && r.json.data?.reporteAsistencia, JSON.stringify(r.json?.errors ?? r.estado))
})
await prueba('E1 · Excel: curador 200, visitante 403', async () => {
  const c = await pedir(BACKEND, '/api/reportes/asistencia/excel', { token: curador })
  const v = await pedir(BACKEND, '/api/reportes/asistencia/excel', { token: visitante })
  exigir(c.estado === 200 && c.tipo.includes('spreadsheetml'), `curador ${c.estado}`)
  exigir(v.estado === 403, `visitante ${v.estado}`)
})

// ---------- Entrega 2: por el gateway ----------
let categorias
await prueba('E2 · categorías', async () => {
  const r = await pedir(GATEWAY, '/api/v2/categorias', { token: curador })
  exigir(r.estado === 200 && r.json.movimientos.length > 0, `estado ${r.estado}`)
  categorias = r.json
  return `${r.json.movimientos.length} movimientos, ${r.json.tecnicas.length} técnicas, ${r.json.tematicas.length} temáticas`
})

await prueba('E2 · seguridad: sin token 401, curador en recorridos 403', async () => {
  const sinToken = await pedir(GATEWAY, '/api/v2/recorridos', { metodo: 'POST', cuerpo: {} })
  const conCurador = await pedir(GATEWAY, '/api/v2/recorridos', { metodo: 'POST', token: curador, cuerpo: {} })
  exigir(sinToken.estado === 401, `sin token ${sinToken.estado}`)
  exigir(conCurador.estado === 403, `curador ${conCurador.estado}`)
})

await prueba('E2 · recorrido personalizado (3 servicios)', async () => {
  const r = await pedir(GATEWAY, '/api/v2/recorridos', {
    metodo: 'POST', token: visitante, cuerpo: { duracionMinutos: 30, usarPreferencias: true },
  })
  exigir(r.estado === 200, `estado ${r.estado}: ${r.json?.mensaje}`)
  exigir(r.json.paradas.length > 0, 'sin paradas')
  exigir(r.json.duracionTotalMinutos <= 30, `duración ${r.json.duracionTotalMinutos} > 30`)
  exigir(r.json.paradas.every((p, i) => p.orden === i + 1 && p.obra?.titulo), 'orden u obras incompletas')
  for (const s of ['UsuariosService', 'CatalogoService', 'MotorService']) exigir(serviciosDe(r.json).includes(s), `no llamó a ${s}`)
  return `${r.json.paradas.length} paradas, ${r.json.duracionTotalMinutos} min · ${trazasLegibles(r.json)}`
})

await prueba('E2 · recorrido con criterios que no matchean nada', async () => {
  const r = await pedir(GATEWAY, '/api/v2/recorridos', {
    metodo: 'POST', token: visitante, cuerpo: { movimientos: ['Movimiento inexistente'] },
  })
  exigir(r.estado === 200 && r.json.paradas.length === 0 && r.json.notas.length > 0, `estado ${r.estado}`)
})

await prueba('E2 · comparación de 3 obras (2 servicios)', async () => {
  const r = await pedir(GATEWAY, '/api/v2/comparaciones', { metodo: 'POST', token: curador, cuerpo: { obraIds: [1, 2, 3] } })
  exigir(r.estado === 200, `estado ${r.estado}: ${r.json?.mensaje}`)
  exigir(r.json.obras.length === 3 && r.json.atributos.length === 8, 'faltan obras o atributos')
  exigir(r.json.atributos.every((a) => a.valores.length === 3), 'algún atributo no tiene un valor por obra')
  for (const s of ['CatalogoService', 'MotorService']) exigir(serviciosDe(r.json).includes(s), `no llamó a ${s}`)
  return `${r.json.observaciones.length} observaciones · ${trazasLegibles(r.json)}`
})

await prueba('E2 · comparación con una sola obra: 400', async () => {
  const r = await pedir(GATEWAY, '/api/v2/comparaciones', { metodo: 'POST', token: curador, cuerpo: { obraIds: [1] } })
  exigir(r.estado === 400, `estado ${r.estado}`)
})

await prueba('E2 · preferencias: guardar y leer', async () => {
  const antes = await pedir(GATEWAY, '/api/v2/preferencias', { token: visitante })
  exigir(antes.estado === 200, `GET ${antes.estado}`)
  const nuevas = { movimientos: [categorias.movimientos[0].nombre], tecnicas: [], tematicas: [] }
  const put = await pedir(GATEWAY, '/api/v2/preferencias', { metodo: 'PUT', token: visitante, cuerpo: nuevas })
  const leidas = await pedir(GATEWAY, '/api/v2/preferencias', { token: visitante })
  // Deja las preferencias como estaban para no alterar la demo.
  await pedir(GATEWAY, '/api/v2/preferencias', { metodo: 'PUT', token: visitante, cuerpo: antes.json })
  exigir(put.estado === 200, `PUT ${put.estado}`)
  exigir(JSON.stringify(leidas.json.movimientos) === JSON.stringify(nuevas.movimientos), 'no se guardaron')
})

await prueba('E2 · interacción: obra inexistente 404, existente 201', async () => {
  const mala = await pedir(GATEWAY, '/api/v2/interacciones', { metodo: 'POST', token: visitante, cuerpo: { obraId: 999999, tipo: 'INTERES' } })
  const buena = await pedir(GATEWAY, '/api/v2/interacciones', { metodo: 'POST', token: visitante, cuerpo: { obraId: 1, tipo: 'VISTA' } })
  exigir(mala.estado === 404, `inexistente ${mala.estado}`)
  exigir(buena.estado === 201, `existente ${buena.estado}`)
})

await prueba('E2 · recomendaciones (4 servicios)', async () => {
  const r = await pedir(GATEWAY, '/api/v2/recomendaciones?cantidad=5', { token: visitante })
  exigir(r.estado === 200, `estado ${r.estado}: ${r.json?.mensaje}`)
  exigir(r.json.recomendaciones.length > 0, 'sin recomendaciones')
  exigir(r.json.recomendaciones.every((x) => x.obra?.titulo && x.motivos.length > 0), 'recomendación sin obra o sin motivos')
  for (const s of ['UsuariosService', 'EventosService', 'CatalogoService', 'MotorService']) exigir(serviciosDe(r.json).includes(s), `no llamó a ${s}`)
  return `${r.json.criterio}, ${r.json.recomendaciones.length} obras · ${trazasLegibles(r.json)}`
})

await prueba('E2 · exploración por movimiento (catálogo + eventos)', async () => {
  const conEventos = categorias.movimientos.map((m) => m.nombre)
  let elegido = null
  for (const movimiento of conEventos) {
    const r = await pedir(GATEWAY, `/api/v2/exploraciones/${encodeURIComponent(movimiento)}`, { token: visitante })
    exigir(r.estado === 200, `estado ${r.estado} en ${movimiento}`)
    exigir(r.json.obras.every((o) => o.movimiento === movimiento), `obras de otro movimiento en ${movimiento}`)
    if (r.json.eventos.length > 0) { elegido = r; break }
  }
  exigir(elegido, 'ningún movimiento tiene eventos vinculados')
  for (const s of ['CatalogoService', 'EventosService']) exigir(serviciosDe(elegido.json).includes(s), `no llamó a ${s}`)
  const j = elegido.json
  return `${j.movimiento}: ${j.obras.length} obras, ${j.artistas.length} artistas, ${j.eventos.length} eventos · ${trazasLegibles(j)}`
})

const fallas = resultados.filter((r) => !r.ok)
for (const r of resultados) console.log(`${r.ok ? 'OK   ' : 'FALLA'} ${r.nombre}${r.detalle ? `  —  ${r.detalle}` : ''}`)
console.log(`\n${resultados.length - fallas.length}/${resultados.length} pruebas OK`)
process.exit(fallas.length ? 1 : 0)
