// Tipos fijos que define el contrato de la API (docs/contrato-api-eventos.md,
// sección "Tipos de evento"). Se centralizan acá para que el reporte y las
// futuras pantallas de eventos usen los mismos valores y el mismo nombre
// legible que después también usa el Excel para nombrar cada hoja.
export const TIPOS_EVENTO = [
  { valor: 'VISITA_GUIADA', etiqueta: 'Visitas guiadas', singular: 'Visita guiada' },
  { valor: 'TALLER', etiqueta: 'Talleres', singular: 'Taller' },
  { valor: 'CHARLA', etiqueta: 'Charlas', singular: 'Charla' }
]

export function etiquetaTipo(valor) {
  // El backend todavía puede devolver textos viejos que no están en la
  // lista fija: en ese caso se muestra el valor tal cual en vez de romper.
  return TIPOS_EVENTO.find((tipo) => tipo.valor === valor)?.etiqueta ?? valor
}

// Para un evento suelto ("Taller"); etiquetaTipo queda para listas y filtros.
export function nombreTipo(valor) {
  return TIPOS_EVENTO.find((tipo) => tipo.valor === valor)?.singular ?? valor
}
