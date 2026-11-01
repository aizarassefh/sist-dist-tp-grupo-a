// Consultas de la Entrega 3 (notificaciones, historial y estado de la
// mensajería). Todas van al backend REST con el mismo token que el resto.
import { pedir } from './cliente'
import type {
  ConsistenciaMensajeria,
  MensajeFallido,
  RegistroHistorial,
  RespuestaNotificaciones,
  TipoNovedad,
} from '../types/modelos'

// La pantalla de notificaciones avisa por este evento cuando cambia algo
// leído/no leído, para que el contador de la navegación no espere a su
// próximo ciclo de 20 segundos. Mismo criterio que EVENTO_SESION_VENCIDA:
// los dos componentes no se conocen entre sí.
export const EVENTO_NOTIFICACIONES_CAMBIARON = 'museo:notificaciones-cambiaron'

export function avisarCambioNotificaciones(): void {
  window.dispatchEvent(new Event(EVENTO_NOTIFICACIONES_CAMBIARON))
}

export function obtenerNotificaciones(
  soloNoLeidas = false,
  limite = 50,
): Promise<RespuestaNotificaciones> {
  return pedir('/api/notificaciones', { parametros: { soloNoLeidas, limite } })
}

export async function marcarNotificacionLeida(id: number): Promise<void> {
  await pedir(`/api/notificaciones/${id}/leida`, { metodo: 'PATCH' })
  avisarCambioNotificaciones()
}

export async function marcarTodasLeidas(): Promise<void> {
  await pedir('/api/notificaciones/leidas', { metodo: 'PATCH' })
  avisarCambioNotificaciones()
}

export function obtenerHistorial(
  filtros: { tipo?: TipoNovedad | ''; idEvento?: string },
  limite = 100,
): Promise<RegistroHistorial[]> {
  return pedir('/api/auditoria/eventos', {
    parametros: { tipo: filtros.tipo, idEvento: filtros.idEvento, limite },
  })
}

export function obtenerConsistencia(): Promise<ConsistenciaMensajeria> {
  return pedir('/api/mensajeria/consistencia')
}

export function obtenerFallidos(): Promise<MensajeFallido[]> {
  return pedir('/api/mensajeria/fallidos', { parametros: { estado: 'PENDIENTE' } })
}

export function reprocesarFallido(id: number): Promise<null> {
  return pedir(`/api/mensajeria/fallidos/${id}/reprocesar`, { metodo: 'POST' })
}
