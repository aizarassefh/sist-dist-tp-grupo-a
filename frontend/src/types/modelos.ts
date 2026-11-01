export type Rol = 'VISITANTE' | 'CURADOR' | 'ADMINISTRADOR'

export interface Usuario {
  id?: number
  nombre?: string
  apellido?: string
  rol?: string | Rol
}

export type TipoNovedad = 'ALTA' | 'MODIFICACION' | 'CANCELACION'

export interface Novedad {
  messageId: string
  type: TipoNovedad
  eventId: number | string
  title: string
  fechaHora: string
  occurredAt: string
  responsibleUserId: number | string
}

export function esNovedad(valor: unknown): valor is Novedad {
  if (typeof valor !== 'object' || valor === null) return false

  const novedad = valor as Record<string, unknown>
  return (
    typeof novedad.messageId === 'string' &&
    (novedad.type === 'ALTA' || novedad.type === 'MODIFICACION' || novedad.type === 'CANCELACION') &&
    (typeof novedad.eventId === 'number' || typeof novedad.eventId === 'string') &&
    typeof novedad.title === 'string' &&
    typeof novedad.fechaHora === 'string' &&
    typeof novedad.occurredAt === 'string' &&
    (typeof novedad.responsibleUserId === 'number' || typeof novedad.responsibleUserId === 'string')
  )
}
