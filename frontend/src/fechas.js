// Fechas y horas de eventos: "hora local de Argentina, formato ISO sin
// zona" (docs/contrato-api-eventos.md). Sin zona en el texto, new Date lo
// interpreta en la hora local del navegador, que alcanza porque el TP corre
// siempre en Argentina.

const FORMATO_FECHA_HORA = new Intl.DateTimeFormat('es-AR', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false
})

const FORMATO_MES_ANIO = new Intl.DateTimeFormat('es-AR', { month: 'long', year: 'numeric' })
const FORMATO_DIA_SEMANA = new Intl.DateTimeFormat('es-AR', { weekday: 'short' })
const FORMATO_HORA = new Intl.DateTimeFormat('es-AR', { hour: '2-digit', minute: '2-digit', hour12: false })

// Piezas sueltas de una fecha, para la agenda: el día en grande, el día de
// la semana, la hora y el mes con que se agrupan los eventos.
export function partesDeFecha(texto) {
  const fecha = new Date(texto)

  return {
    dia: fecha.getDate(),
    diaSemana: FORMATO_DIA_SEMANA.format(fecha).replace('.', ''),
    hora: FORMATO_HORA.format(fecha),
    mesAnio: FORMATO_MES_ANIO.format(fecha)
  }
}

export function formatearFechaHora(texto) {
  return FORMATO_FECHA_HORA.format(new Date(texto))
}

export function yaComenzo(fechaHora) {
  return new Date(fechaHora) <= new Date()
}

// El input datetime-local da "2026-10-03T15:00" y el contrato pide
// "2026-10-03T15:00:00": agregar/sacar los segundos es la única diferencia.
export function aFechaHoraContrato(valorInput) {
  return valorInput ? `${valorInput}:00` : ''
}

export function aValorInput(fechaHoraContrato) {
  return fechaHoraContrato ? fechaHoraContrato.slice(0, 16) : ''
}
