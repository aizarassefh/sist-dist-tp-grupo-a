import { gql } from '@apollo/client'

export const OBRAS_LISTADO_QUERY = gql`
  query ObtenerObrasListado(
    $filtro: FiltroObras
    $pagina: Int
    $tamanio: Int
  ) {
    obras(
      filtro: $filtro
      pagina: $pagina
      tamanio: $tamanio
    ) {
      id
      titulo
      artista {
        nombre
      }
      imagenUrl
      anioCreacion
      epoca
      ubicacion
      enExhibicion
    }
  }
`

// El schema no tiene una consulta "obra(id)", solo "obras(filtro, pagina, tamanio)".
// Por eso el detalle pide la lista completa (tamanio: 100) y busca por id en el
// cliente. Cuando el backend agregue "obra(id: ID!)" hay que usar esa en su lugar.
export const OBRA_DETALLE_QUERY = gql`
  query ObtenerObrasDetalle(
    $filtro: FiltroObras
    $pagina: Int
    $tamanio: Int
  ) {
    obras(
      filtro: $filtro
      pagina: $pagina
      tamanio: $tamanio
    ) {
      id
      titulo
      artista {
        nombre
        biografia
      }
      imagenUrl
      anioCreacion
      tecnica
      dimensiones
      epoca
      descripcion
      ubicacion
      enExhibicion
      comentarios {
        usuario
        texto
        fecha
      }
    }
  }
`
export const REPORTE_ASISTENCIA_QUERY = gql`
  query ObtenerReporteAsistencia(
    $filtro: FiltroReporte
  ) {
    reporteAsistencia(filtro: $filtro) {
      fechaCorte

      grupos {
        mes
        tipo
        cantidadDeEventos
        totalInscriptosAcumulados
        promedioDeAsistencia

        eventosMasPopulares {
          id
          titulo
          cantidadInscriptos
        }
      }
    }
  }
`