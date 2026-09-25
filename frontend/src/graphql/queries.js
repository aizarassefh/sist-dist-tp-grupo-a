import { gql } from '@apollo/client'

export const OBRAS_QUERY = gql`
  query ObtenerObras(
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