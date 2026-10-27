// Salida hacia el API Gateway (Entrega 2). Reusa pedir() de cliente.js, así
// el token, el aviso de sesión vencida (401) y la forma de los errores son
// los mismos que en el resto del front.
import { ErrorApi, mensajeDeError, pedir } from './cliente'

// Se puede cambiar con VITE_GATEWAY_URL sin tocar código; el default es el
// puerto del gateway (docs/contrato-api-gateway.md).
export const GATEWAY_URL = import.meta.env.VITE_GATEWAY_URL ?? 'http://localhost:8080'

function pedirAlGateway(ruta, opciones = {}) {
  return pedir(ruta, { ...opciones, base: GATEWAY_URL, conMensajeServidor: true })
}

// Para 400, 404, 500 y 503 el gateway manda un "mensaje" pensado para
// mostrarse (por ejemplo "El servicio X no está disponible"); se usa ese. Con
// 401 y 403 se mantienen los textos de siempre, y sin respuesta (gateway
// caído) queda el mensaje de conexión.
export function mensajeDeErrorGateway(error) {
  if (error instanceof ErrorApi && error.mensajeServidor && error.estado !== 401 && error.estado !== 403) {
    return error.mensajeServidor
  }
  return mensajeDeError(error)
}

export function generarRecorrido(pedido) {
  return pedirAlGateway('/api/v2/recorridos', { metodo: 'POST', cuerpo: pedido })
}

export function compararObras(obraIds) {
  return pedirAlGateway('/api/v2/comparaciones', { metodo: 'POST', cuerpo: { obraIds } })
}

export function obtenerRecomendaciones(cantidad) {
  return pedirAlGateway('/api/v2/recomendaciones', { parametros: { cantidad } })
}

export function obtenerPreferencias() {
  return pedirAlGateway('/api/v2/preferencias')
}

export function guardarPreferencias(preferencias) {
  return pedirAlGateway('/api/v2/preferencias', { metodo: 'PUT', cuerpo: preferencias })
}

// tipo: 'VISTA' o 'INTERES'
export function registrarInteraccion(obraId, tipo) {
  return pedirAlGateway('/api/v2/interacciones', { metodo: 'POST', cuerpo: { obraId, tipo } })
}

export function explorarMovimiento(movimiento) {
  return pedirAlGateway(`/api/v2/exploraciones/${encodeURIComponent(movimiento)}`)
}

export function obtenerCategorias() {
  return pedirAlGateway('/api/v2/categorias')
}
