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
