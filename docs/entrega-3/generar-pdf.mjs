// Genera Entrega-3-Grupo-A.pdf a partir de entrega-3.html.
// Uso: node docs/entrega-3/generar-pdf.mjs
//
// El anexo se arma en este momento con mensajes-reales.json: novedades leídas
// del topic eventos.novedades durante las pruebas, tal como las publicó el
// backend. Así el documento muestra mensajes reales y no ejemplos escritos a mano.
// Imprime con Chrome o Edge sin interfaz: no hace falta instalar nada.
import { readFileSync, writeFileSync, existsSync, rmSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { execFileSync } from 'node:child_process'

const dirDoc = dirname(fileURLToPath(import.meta.url))
const salidaPdf = join(dirDoc, 'Entrega-3-Grupo-A.pdf')
// El HTML armado va al lado del original para que resuelva logo y capturas
// con las mismas rutas relativas.
const htmlArmado = join(dirDoc, '.entrega-3-armada.html')

const escapar = (texto) =>
  texto.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')

const archivoMensajes = join(dirDoc, 'mensajes-reales.json')
const anexo = existsSync(archivoMensajes)
  ? JSON.parse(readFileSync(archivoMensajes, 'utf8'))
      .map((m) => `<h3>${escapar(m.titulo)}</h3>\n<pre>${escapar(JSON.stringify(m.mensaje, null, 2))}</pre>`)
      .join('\n')
  : '<p class="completar">[Falta mensajes-reales.json]</p>'

const html = readFileSync(join(dirDoc, 'entrega-3.html'), 'utf8')
if (!html.includes('<!-- ANEXO_MENSAJES -->')) {
  throw new Error('entrega-3.html no tiene la marca <!-- ANEXO_MENSAJES -->')
}
writeFileSync(htmlArmado, html.replace('<!-- ANEXO_MENSAJES -->', anexo))

const navegadores = [
  process.env.CHROME,
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  '/usr/bin/google-chrome',
  '/usr/bin/chromium',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
].filter(Boolean)
const navegador = navegadores.find((ruta) => existsSync(ruta))
if (!navegador) {
  throw new Error('No encontré Chrome ni Edge. Indicá la ruta con la variable CHROME.')
}

try {
  execFileSync(navegador, [
    '--headless=new',
    '--disable-gpu',
    '--no-pdf-header-footer',
    '--allow-file-access-from-files',
    `--print-to-pdf=${salidaPdf}`,
    `file:///${htmlArmado.replaceAll('\\', '/')}`,
  ], { stdio: 'inherit' })
} finally {
  rmSync(htmlArmado, { force: true })
}
console.log(`PDF generado: ${salidaPdf}`)
