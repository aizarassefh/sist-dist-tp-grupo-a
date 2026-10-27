// Genera Entrega-2-Grupo-A.pdf a partir de entrega-2.html.
// Uso: node docs/entrega-2/generar-pdf.mjs
//
// El anexo con los .proto se arma en este momento leyendo la carpeta proto/,
// para que el documento nunca muestre un contrato distinto del que compila.
// Imprime con Chrome o Edge sin interfaz: no hace falta instalar nada.
import { readFileSync, writeFileSync, existsSync, readdirSync, rmSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { execFileSync } from 'node:child_process'

const dirDoc = dirname(fileURLToPath(import.meta.url))
const dirProto = join(dirDoc, '..', '..', 'proto')
const salidaPdf = join(dirDoc, 'Entrega-2-Grupo-A.pdf')
// El HTML armado va al lado del original para que resuelva logo y capturas
// con las mismas rutas relativas.
const htmlArmado = join(dirDoc, '.entrega-2-armada.html')

const escapar = (texto) =>
  texto.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')

const anexo = readdirSync(dirProto)
  .filter((archivo) => archivo.endsWith('.proto'))
  .sort()
  .map((archivo) => {
    const contenido = readFileSync(join(dirProto, archivo), 'utf8').replaceAll('\r\n', '\n')
    return `<h3>proto/${archivo}</h3>\n<pre>${escapar(contenido)}</pre>`
  })
  .join('\n')

const html = readFileSync(join(dirDoc, 'entrega-2.html'), 'utf8')
if (!html.includes('<!-- ANEXO_PROTOS -->')) {
  throw new Error('entrega-2.html no tiene la marca <!-- ANEXO_PROTOS -->')
}
writeFileSync(htmlArmado, html.replace('<!-- ANEXO_PROTOS -->', anexo))

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
