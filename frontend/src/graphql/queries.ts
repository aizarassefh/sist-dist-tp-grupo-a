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

export const OBRA_DETALLE_QUERY = gql`
  query ObtenerObra($id: ID!) {
    obra(id: $id) {
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

export const NOVEDADES_SUBSCRIPTION = gql`
  subscription Novedades {
    novedades {
      messageId
      type
      eventId
      title
      fechaHora
      occurredAt
      responsibleUserId
    }
  }
`
