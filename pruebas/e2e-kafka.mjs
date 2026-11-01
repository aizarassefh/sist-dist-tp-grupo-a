// Prueba de punta a punta de la mensajería (Entrega 3) contra los procesos
// reales: backend principal, consumidor de notificaciones, consumidor de
// auditoría y Kafka. Verifica que las operaciones sobre eventos publiquen
// novedades y que los consumidores, en otros procesos, las procesen de forma
// asíncrona; además inyecta en Kafka un duplicado y un mensaje inválido.
//
// Uso (con todo levantado): node pruebas/e2e-kafka.mjs
// Variables: BACKEND_URL (http://localhost:8000), GATEWAY_URL (http://localhost:8080), KAFKA_HOME (kafka/kafka_2.13-3.7.0
// del repo), MUSEO_CLAVE (por defecto, la clave de ejemplo del README).
import { readFileSync, existsSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import { randomUUID } from 'node:crypto'

const raiz = join(dirname(fileURLToPath(import.meta.url)), '..')
const BACKEND = process.env.BACKEND_URL ?? 'http://localhost:8000'
const GATEWAY = process.env.GATEWAY_URL ?? 'http://localhost:8080'
const KAFKA_HOME = process.env.KAFKA_HOME ?? join(raiz, 'kafka', 'kafka_2.13-3.7.0')
const TOPIC = 'eventos.novedades'

function claveDeEjemplo() {
  if (process.env.MUSEO_CLAVE) return process.env.MUSEO_CLAVE
  const encontrada = readFileSync(join(raiz, 'README.md'), 'utf8').match(/Contraseña para todos\*\*: `([^`]+)`/)
  if (!encontrada) throw new Error('No encontré la clave de ejemplo en el README; usá MUSEO_CLAVE')
  return encontrada[1]
}

const resultados = []
async function prueba(nombre, fn) {
  try {
    resultados.push({ nombre, ok: true, detalle: (await fn()) ?? '' })
  } catch (error) {
    resultados.push({ nombre, ok: false, detalle: error.message })
  }
}
function exigir(condicion, mensaje) {
  if (!condicion) throw new Error(mensaje)
}

async function pedir(ruta, { metodo = 'GET', token, cuerpo } = {}) {
  const respuesta = await fetch(BACKEND + ruta, {
    method: metodo,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(cuerpo ? { 'Content-Type': 'application/json' } : {}),
    },
    body: cuerpo ? JSON.stringify(cuerpo) : undefined,
  })
  const texto = await respuesta.text()
  let json = null
  try { json = texto ? JSON.parse(texto) : null } catch { /* sin cuerpo JSON */ }
  return { estado: respuesta.status, json }
}

// La consistencia es eventual: lo que escriben los consumidores aparece unos
// instantes después de la operación. Se espera con un límite en vez de un sleep fijo.
async function esperar(descripcion, fn, timeoutMs = 30000) {
  const inicio = Date.now()
  for (;;) {
    const valor = await fn()
    if (valor) return { valor, ms: Date.now() - inicio }
    if (Date.now() - inicio > timeoutMs) throw new Error(`Pasaron ${timeoutMs / 1000} s sin ${descripcion}`)
    await new Promise((r) => setTimeout(r, 500))
  }
}

function publicarEnKafka(lineas) {
  exigir(existsSync(join(KAFKA_HOME, 'libs')), `No encuentro Kafka en ${KAFKA_HOME} (KAFKA_HOME)`)
  const r = spawnSync('java', [
    '-cp', join(KAFKA_HOME, 'libs', '*'),
    `-Dlog4j.configuration=file:${join(KAFKA_HOME, 'config', 'tools-log4j.properties')}`,
    'kafka.tools.ConsoleProducer', '--bootstrap-server', 'localhost:9092', '--topic', TOPIC,
  ], { input: lineas.join('\n') + '\n', encoding: 'utf8' })
  exigir(r.status === 0, `ConsoleProducer terminó con ${r.status}: ${r.stderr}`)
}

async function login(email) {
  const r = await pedir('/api/auth/login', { metodo: 'POST', cuerpo: { email, password: claveDeEjemplo() } })
  exigir(r.estado === 200, `login de ${email} devolvió ${r.estado}`)
  return r.json.accessToken
}

const curador = await login('curador@test.com')
const admin = await login('admin@test.com')
const visitante = await login('visitante@test.com')
const idVisitante = (await pedir('/api/auth/me', { token: visitante })).json.id
const idCurador = (await pedir('/api/usuarios/curadores', { token: curador })).json[0].id

const notificacionesDe = async (idEvento, tipo) =>
  (await pedir('/api/notificaciones?limite=200', { token: visitante })).json.notificaciones
    .filter((n) => n.idEvento === idEvento && (!tipo || n.tipo === tipo))
const historialDe = async (idEvento) =>
  (await pedir(`/api/auditoria/eventos?idEvento=${idEvento}&limite=50`, { token: curador })).json
const consistencia = async () => (await pedir('/api/mensajeria/consistencia', { token: curador })).json

// Las preferencias del visitante son de la Entrega 2 y viven detrás del gateway.
// Se le pone Impresionismo para que el alta le llegue, y al final se restauran.
async function preferencias(metodo, cuerpo) {
  const r = await fetch(`${GATEWAY}/api/v2/preferencias`, {
    method: metodo,
    headers: { Authorization: `Bearer ${visitante}`, ...(cuerpo ? { 'Content-Type': 'application/json' } : {}) },
    body: cuerpo ? JSON.stringify(cuerpo) : undefined,
  })
  exigir(r.ok, `${metodo} /api/v2/preferencias devolvió ${r.status} (¿está levantado el gateway?)`)
  return r.json()
}
const preferenciasOriginales = await preferencias('GET')
await preferencias('PUT', { ...preferenciasOriginales, movimientos: [...new Set([...preferenciasOriginales.movimientos, 'Impresionismo'])] })

const fecha = new Date(Date.now() + 20 * 24 * 3600 * 1000)
const fechaHora = `${fecha.toISOString().slice(0, 10)}T18:00:00`
const cuerpoEvento = {
  titulo: `Charla de prueba e2e ${Date.now()}`, descripcion: 'Evento creado por pruebas/e2e-kafka.mjs',
  tipo: 'CHARLA', fechaHora, duracionMinutos: 60, cupoMaximo: 20, curadorId: idCurador, movimiento: 'Impresionismo',
}
const generadosAntes = (await consistencia()).generados
let idEvento

await prueba('Alta: publica y notifica a los visitantes que prefieren el movimiento', async () => {
  const r = await pedir('/api/eventos', { metodo: 'POST', token: curador, cuerpo: cuerpoEvento })
  exigir(r.estado === 201, `alta devolvió ${r.estado}: ${JSON.stringify(r.json)}`)
  idEvento = r.json.id
  const { valor, ms } = await esperar('la notificación de alta', async () => (await notificacionesDe(idEvento, 'ALTA'))[0])
  return `evento ${idEvento}; notificación a los ${ms} ms: "${valor.mensaje}"`
})

await prueba('Modificación: notifica a los inscriptos con los campos que cambiaron', async () => {
  const insc = await pedir(`/api/eventos/${idEvento}/inscripcion`, { metodo: 'POST', token: visitante })
  exigir(insc.estado === 201, `inscripción devolvió ${insc.estado}`)
  const r = await pedir(`/api/eventos/${idEvento}`, {
    metodo: 'PUT', token: curador, cuerpo: { ...cuerpoEvento, fechaHora: `${fecha.toISOString().slice(0, 10)}T19:30:00`, cupoMaximo: 35 },
  })
  exigir(r.estado === 200, `edición devolvió ${r.estado}`)
  const { valor, ms } = await esperar('la notificación de modificación', async () => (await notificacionesDe(idEvento, 'MODIFICACION'))[0])
  return `a los ${ms} ms: "${valor.mensaje}"`
})

await prueba('Edición sin cambios: no publica nada', async () => {
  const antes = (await consistencia()).generados
  const actual = await pedir(`/api/eventos/${idEvento}`, { token: curador })
  const e = actual.json
  const r = await pedir(`/api/eventos/${idEvento}`, {
    metodo: 'PUT', token: curador,
    cuerpo: { titulo: e.titulo, descripcion: e.descripcion, tipo: e.tipo, fechaHora: e.fechaHora, duracionMinutos: e.duracionMinutos,
              cupoMaximo: e.cupoMaximo, curadorId: e.curadorResponsable.id, movimiento: e.movimiento },
  })
  exigir(r.estado === 200, `edición devolvió ${r.estado}`)
  const despues = (await consistencia()).generados
  exigir(despues === antes, `se generaron ${despues - antes} mensajes`)
})

await prueba('Historial: registra alta y modificación con datos anteriores y posteriores', async () => {
  const { valor } = await esperar('las dos filas del historial', async () => {
    const h = await historialDe(idEvento)
    return h.length >= 2 ? h : null
  })
  const mod = valor.find((x) => x.tipo === 'MODIFICACION')
  exigir(mod, 'no está la modificación')
  exigir(mod.camposModificados.includes('fechaHora') && mod.camposModificados.includes('cupoMaximo'), `campos: ${mod.camposModificados}`)
  exigir(mod.datosAnteriores.cupoMaximo === 20 && mod.datosPosteriores.cupoMaximo === 35, 'anterior/posterior no coinciden')
  exigir(mod.usuarioResponsable?.id === idCurador, 'responsable incorrecto')
  return `${valor.length} registros; cambiaron ${mod.camposModificados.join(', ')}`
})

await prueba('Cancelación: avisa a quienes estaban inscriptos', async () => {
  const r = await pedir(`/api/eventos/${idEvento}`, { metodo: 'DELETE', token: curador })
  exigir(r.estado === 204, `borrado devolvió ${r.estado}`)
  const { valor, ms } = await esperar('la notificación de cancelación', async () => (await notificacionesDe(idEvento, 'CANCELACION'))[0])
  const h = await esperar('la cancelación en el historial', async () => (await historialDe(idEvento)).find((x) => x.tipo === 'CANCELACION'))
  exigir(h.valor.datosAnteriores.idsInscriptos.includes(idVisitante), 'la foto anterior no trae al inscripto')
  return `a los ${ms} ms: "${valor.mensaje}"`
})

await prueba('Seguridad: cada rol ve solo lo suyo', async () => {
  const casos = [
    ['/api/auditoria/eventos', visitante, 403], ['/api/mensajeria/consistencia', visitante, 403],
    ['/api/notificaciones', curador, 403], ['/api/notificaciones', null, 401],
  ]
  for (const [ruta, token, esperado] of casos) {
    const r = await pedir(ruta, { token })
    exigir(r.estado === esperado, `${ruta} devolvió ${r.estado} en vez de ${esperado}`)
  }
  const reproceso = await pedir('/api/mensajeria/fallidos/1/reprocesar', { metodo: 'POST', token: curador })
  exigir(reproceso.estado === 403, `reprocesar como curador devolvió ${reproceso.estado}`)
})

await prueba('Duplicado inyectado en Kafka: no duplica notificaciones ni historial', async () => {
  const idFalso = 900000 + Math.floor(Math.random() * 99999)
  const foto = { id: idFalso, titulo: 'Evento de prueba de duplicados', descripcion: '-', tipo: 'TALLER', fechaHora,
                 duracionMinutos: 30, cupoMaximo: 10, movimiento: null, idCurador, nombreCurador: 'Juan Perez', idsInscriptos: [idVisitante] }
  const mensaje = JSON.stringify({
    idMensaje: randomUUID(), versionEsquema: 1, tipo: 'MODIFICACION', idEvento: idFalso, tituloEvento: foto.titulo,
    fechaHoraEvento: fechaHora, fechaOperacion: new Date().toISOString(), idUsuarioResponsable: idCurador,
    nombreUsuarioResponsable: 'Juan Perez', eventoAnterior: { ...foto, cupoMaximo: 5 }, eventoPosterior: foto, camposModificados: ['cupoMaximo'],
  })
  publicarEnKafka([mensaje, mensaje, mensaje])
  await esperar('el historial del mensaje inyectado', async () => (await historialDe(idFalso)).length > 0)
  await new Promise((r) => setTimeout(r, 4000))
  const h = await historialDe(idFalso)
  const n = await notificacionesDe(idFalso)
  exigir(h.length === 1, `${h.length} filas de historial`)
  exigir(n.length === 1, `${n.length} notificaciones`)
  return 'publicado 3 veces: 1 fila de historial y 1 notificación'
})

await prueba('Mensaje inválido: va al DLT y queda en fallidos, sin efectos', async () => {
  const antes = (await pedir('/api/mensajeria/fallidos?estado=PENDIENTE', { token: admin })).json.length
  publicarEnKafka([JSON.stringify({ versionEsquema: 1, tipo: 'ALTA', idEvento: 999, tituloEvento: 'sin idMensaje' })])
  const { valor, ms } = await esperar('el mensaje en fallidos', async () => {
    const f = (await pedir('/api/mensajeria/fallidos?estado=PENDIENTE', { token: admin })).json
    return f.length >= antes + 2 ? f : null
  })
  return `a los ${ms} ms hay ${valor.length - antes} fallidos nuevos (uno por consumidor): "${valor[0].error.slice(0, 80)}"`
})

await prueba('Consistencia eventual: todo lo publicado fue procesado por ambos consumidores', async () => {
  const { valor, ms } = await esperar('consistencia sin pendientes ni faltantes', async () => {
    const c = await consistencia()
    return c.pendientesDePublicar === 0 && c.consumidores.every((x) => x.faltantes.length === 0) ? c : null
  })
  exigir(valor.generados >= generadosAntes + 3, `generados ${valor.generados}`)
  return `a los ${ms} ms: ${valor.publicados} publicados; ` +
    valor.consumidores.map((x) => `${x.consumidor} ${x.procesados}`).join(', ') + `; fallidos pendientes ${valor.fallidosPendientes}`
})

await preferencias('PUT', preferenciasOriginales)

const fallas = resultados.filter((r) => !r.ok)
for (const r of resultados) console.log(`${r.ok ? 'OK   ' : 'FALLA'} ${r.nombre}${r.detalle ? `  —  ${r.detalle}` : ''}`)
console.log(`\n${resultados.length - fallas.length}/${resultados.length} pruebas OK`)
process.exit(fallas.length ? 1 : 0)
