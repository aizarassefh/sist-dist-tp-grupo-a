// Único punto de salida hacia el backend REST. Centraliza la URL, el token y
// el manejo de errores para que ninguna pantalla repita esa lógica.

// Se puede cambiar con VITE_API_URL sin tocar código; el default es el
// server.port de application.properties.
export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8000'

const CLAVE_TOKEN = 'token'

// Se avisa por un evento del navegador y no llamando a React directamente:
// así este módulo no depende de ningún componente y lo pueden usar también
// Apollo y cualquier pantalla.
export const EVENTO_SESION_VENCIDA = 'museo:sesion-vencida'

export function obtenerToken() {
  return localStorage.getItem(CLAVE_TOKEN)
}

export function guardarToken(token) {
  localStorage.setItem(CLAVE_TOKEN, token)
}

export function borrarToken() {
  localStorage.removeItem(CLAVE_TOKEN)
}

export function avisarSesionVencida() {
  borrarToken()
  window.dispatchEvent(new Event(EVENTO_SESION_VENCIDA))
}

export class ErrorApi extends Error {
  constructor(estado, mensaje) {
    super(mensaje)
    this.estado = estado
  }
}

// El backend hoy devuelve el motivo real del error en un campo equivocado
// (GlobalExceptionHandler pasa los argumentos desordenados a ErrorResponse),
// así que las pantallas deciden el texto según el código HTTP y no según el
// cuerpo de la respuesta.
const MENSAJES_POR_ESTADO = {
  400: 'Los datos enviados no son válidos.',
  401: 'Tu sesión venció. Volvé a iniciar sesión.',
  403: 'No tenés permisos para realizar esta acción.',
  404: 'No se encontró lo que buscabas.',
  409: 'La operación entra en conflicto con el estado actual.',
}

export function mensajeDeError(error, mensajesPropios = {}) {
  if (error instanceof ErrorApi) {
    return (
      mensajesPropios[error.estado] ??
      MENSAJES_POR_ESTADO[error.estado] ??
      'Ocurrió un error en el servidor. Probá de nuevo en unos minutos.'
    )
  }
  return 'No se pudo conectar con el servidor.'
}

/**
 * Hace una petición al backend REST.
 *
 * @param {string} ruta ruta relativa, por ejemplo "/api/eventos"
 * @param {object} [opciones]
 * @param {string} [opciones.metodo] verbo HTTP, GET por defecto
 * @param {object} [opciones.cuerpo] se envía como JSON
 * @param {object} [opciones.parametros] query string; se omiten vacíos y null
 * @param {boolean} [opciones.comoArchivo] devuelve un Blob, para descargas
 */
export async function pedir(ruta, { metodo = 'GET', cuerpo, parametros, comoArchivo = false } = {}) {
  const token = obtenerToken()
  const headers = {}

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  if (cuerpo !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  const respuesta = await fetch(API_URL + ruta + armarQuery(parametros), {
    method: metodo,
    headers,
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  })

  // Un 401 con token significa que venció o fue alterado. Sin token es un
  // login fallido, que lo maneja la pantalla de login.
  if (respuesta.status === 401 && token) {
    avisarSesionVencida()
  }

  if (!respuesta.ok) {
    throw new ErrorApi(respuesta.status, `HTTP ${respuesta.status}`)
  }

  if (comoArchivo) {
    return respuesta.blob()
  }

  // Algunos endpoints devuelven texto plano con Content-Type JSON (por
  // ejemplo el registro), así que se intenta parsear y si falla se devuelve
  // el texto tal cual.
  const texto = await respuesta.text()
  if (!texto) {
    return null
  }
  try {
    return JSON.parse(texto)
  } catch {
    return texto
  }
}

function armarQuery(parametros) {
  if (!parametros) {
    return ''
  }
  const query = new URLSearchParams()
  for (const [clave, valor] of Object.entries(parametros)) {
    if (valor !== undefined && valor !== null && valor !== '') {
      query.append(clave, valor)
    }
  }
  const texto = query.toString()
  return texto ? `?${texto}` : ''
}
