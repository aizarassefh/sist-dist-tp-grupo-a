export type Rol = 'VISITANTE' | 'CURADOR' | 'ADMINISTRADOR'

export interface Usuario {
  id?: number
  nombre?: string
  apellido?: string
  rol?: string | Rol
}

export type TipoNovedad = 'ALTA' | 'MODIFICACION' | 'CANCELACION'

export interface EventoDto {
  id: number
  titulo: string
  descripcion: string
  tipo: string
  fechaHora: string
  duracionMinutos: number
  cupoMaximo: number
  // Opcional: los eventos anteriores a la Entrega 3 no lo tienen.
  movimiento?: string | null
  curadorResponsable: { id: number; nombre: string }
}

// Notificaciones del visitante (GET /api/notificaciones)
export interface Notificacion {
  id: number
  tipo: TipoNovedad
  idEvento: number
  tituloEvento: string
  mensaje: string
  fechaOperacion: string
  leida: boolean
}

export interface RespuestaNotificaciones {
  noLeidas: number
  notificaciones: Notificacion[]
}

// Foto del evento que viaja dentro de la novedad (docs/contrato-mensajeria.md)
export interface EventoHistorial {
  id: number
  titulo: string
  descripcion: string
  tipo: string
  fechaHora: string
  duracionMinutos: number
  cupoMaximo: number
  movimiento: string | null
  idCurador: number
  nombreCurador: string
  idsInscriptos: number[]
}

export interface RegistroHistorial {
  idMensaje: string
  tipo: TipoNovedad
  idEvento: number
  tituloEvento: string
  fechaHoraEvento: string
  fechaOperacion: string
  usuarioResponsable: { id: number; nombre: string } | null
  camposModificados: string[]
  datosAnteriores: EventoHistorial | null
  datosPosteriores: EventoHistorial | null
  fechaProcesamiento: string
}

export interface EstadoConsumidor {
  consumidor: string
  procesados: number
  faltantes: string[]
}

export interface ConsistenciaMensajeria {
  generados: number
  publicados: number
  pendientesDePublicar: number
  consumidores: EstadoConsumidor[]
  fallidosPendientes: number
  consistente: boolean
}

export interface MensajeFallido {
  id: number
  idMensaje: string | null
  consumidor: string
  error: string
  contenido: string
  fechaRecepcion: string
  estado: 'PENDIENTE' | 'REPROCESADO'
}
