// Conversión entre los filtros de eventos (URL de /eventos, contrato de
// filtros favoritos) y los distintos formatos que necesita cada pantalla.
// Se extrae acá porque tanto Eventos como FiltrosFavoritos arman y leen
// estos mismos 5 campos (docs/contrato-api-eventos.md).
export const CAMPOS_FILTRO_EVENTOS = ['desde', 'hasta', 'tipo', 'curadorId', 'estado']

// Lee los filtros desde un searchParams de useSearchParams: siempre strings,
// '' si el parámetro no está.
export function filtrosDesdeBusqueda(searchParams) {
  const filtros = {}
  for (const campo of CAMPOS_FILTRO_EVENTOS) {
    filtros[campo] = searchParams.get(campo) ?? ''
  }
  return filtros
}

// Arma el query string (sin "?") para navegar a /eventos con estos filtros.
export function busquedaDesdeFiltros(filtros) {
  const params = new URLSearchParams()
  for (const campo of CAMPOS_FILTRO_EVENTOS) {
    const valor = filtros[campo]
    if (valor !== undefined && valor !== null && valor !== '') {
      params.set(campo, valor)
    }
  }
  return params.toString()
}

// Convierte los filtros (strings de la URL) al objeto "filtros" del
// contrato de filtros favoritos: vacíos van en null, curadorId es número.
export function filtrosParaGuardar(filtros) {
  return {
    desde: filtros.desde || null,
    hasta: filtros.hasta || null,
    tipo: filtros.tipo || null,
    curadorId: filtros.curadorId ? Number(filtros.curadorId) : null,
    estado: filtros.estado || null
  }
}
