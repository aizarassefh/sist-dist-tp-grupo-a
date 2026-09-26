import { useState } from 'react'
import { useQuery } from '@apollo/client/react'
import { REPORTE_ASISTENCIA_QUERY } from '../graphql/queries'
import { pedir, mensajeDeError } from '../api/cliente'
import { TIPOS_EVENTO, etiquetaTipo } from '../tiposEvento'
import { formatearFechaHora, formatearMes } from '../fechas'

// El contrato (docs/contrato-api-eventos.md) propone mover esta ruta a
// /api/reportes/asistencia/excel cuando el backend pase la exportación al
// módulo de reportes en vez del módulo de eventos.
const RUTA_EXPORTAR_EXCEL = '/api/eventos/exportar'

function ReporteAsistencia() {
  const [filtros, setFiltros] = useState({
    desde: '',
    hasta: '',
    tipo: '',
    estado: 'TODOS',
    agruparPor: 'MES'
  })

  const [filtrosAplicados, setFiltrosAplicados] = useState({
    desde: null,
    hasta: null,
    tipo: null,
    estado: 'TODOS',
    agruparPor: 'MES'
  })

  const { loading, error, data } = useQuery(
    REPORTE_ASISTENCIA_QUERY,
    {
      variables: {
        filtro: filtrosAplicados
      }
    }
  )

  const [exportando, setExportando] = useState(false)
  const [errorExportacion, setErrorExportacion] = useState(null)

  function cambiarFiltro(nombre, valor) {
    setFiltros((filtrosActuales) => ({
      ...filtrosActuales,
      [nombre]: valor
    }))
  }

  function generarReporte(e) {
    e.preventDefault()

    setFiltrosAplicados({
      desde: filtros.desde || null,
      hasta: filtros.hasta || null,
      tipo: filtros.tipo || null,
      estado: filtros.estado,
      agruparPor: filtros.agruparPor
    })
  }

  async function exportarExcel() {
    setExportando(true)
    setErrorExportacion(null)

    try {
      // Se exporta con los filtros ya aplicados al reporte, no con lo que
      // el usuario esté editando sin confirmar todavía.
      const archivo = await pedir(RUTA_EXPORTAR_EXCEL, {
        parametros: {
          desde: filtrosAplicados.desde,
          hasta: filtrosAplicados.hasta,
          tipo: filtrosAplicados.tipo,
          estado: filtrosAplicados.estado
        },
        comoArchivo: true
      })

      const url = URL.createObjectURL(archivo)
      const enlace = document.createElement('a')
      // toISOString daría la fecha en UTC: después de las 21 en Argentina
      // el archivo saldría con la fecha de mañana. sv-SE formatea AAAA-MM-DD.
      const fecha = new Date().toLocaleDateString('sv-SE')

      enlace.href = url
      enlace.download = `reporte-asistencia-${fecha}.xlsx`
      document.body.appendChild(enlace)
      enlace.click()
      enlace.remove()
      URL.revokeObjectURL(url)
    } catch (error) {
      setErrorExportacion(
        mensajeDeError(error, {
          403: 'Solo curadores y administradores pueden exportar.'
        })
      )
    } finally {
      setExportando(false)
    }
  }

  return (
    <section className="reporte">
      <header className="encabezado">
        <h1>Reporte de asistencia</h1>

        <p>
          Eventos e inscripciones agrupados por mes o por tipo de
          evento.
        </p>

        <p className="reporte-aclaracion">
          La asistencia se calcula con las inscripciones
          registradas: el sistema no registra la presencia
          el día del evento.
        </p>
      </header>

      <form
        className="filtros reporte-filtros"
        onSubmit={generarReporte}
      >
        <div className="campo">
          <label htmlFor="desde">
            Desde
          </label>

          <input
            id="desde"
            type="date"
            value={filtros.desde}
            onChange={(e) =>
              cambiarFiltro('desde', e.target.value)
            }
          />
        </div>

        <div className="campo">
          <label htmlFor="hasta">
            Hasta
          </label>

          <input
            id="hasta"
            type="date"
            value={filtros.hasta}
            onChange={(e) =>
              cambiarFiltro('hasta', e.target.value)
            }
          />
        </div>

        <div className="campo">
          <label htmlFor="tipo">
            Tipo de evento
          </label>

          <select
            id="tipo"
            value={filtros.tipo}
            onChange={(e) =>
              cambiarFiltro('tipo', e.target.value)
            }
          >
            <option value="">Todos</option>

            {TIPOS_EVENTO.map((tipo) => (
              <option key={tipo.valor} value={tipo.valor}>
                {tipo.etiqueta}
              </option>
            ))}
          </select>
        </div>

        <div className="campo">
          <label htmlFor="estado">
            Estado
          </label>

          <select
            id="estado"
            value={filtros.estado}
            onChange={(e) =>
              cambiarFiltro('estado', e.target.value)
            }
          >
            <option value="TODOS">Todos</option>
            <option value="PASADOS">Pasados</option>
            <option value="FUTUROS">Futuros</option>
          </select>
        </div>

        <div className="campo">
          <label htmlFor="agruparPor">
            Agrupar por
          </label>

          <select
            id="agruparPor"
            value={filtros.agruparPor}
            onChange={(e) =>
              cambiarFiltro(
                'agruparPor',
                e.target.value
              )
            }
          >
            <option value="MES">
              Mes
            </option>

            <option value="TIPO">
              Tipo
            </option>

            <option value="MES_Y_TIPO">
              Mes y tipo
            </option>
          </select>
        </div>

        <div className="reporte-acciones">
          <button
            className="boton-generar"
            type="submit"
          >
            Generar reporte
          </button>

          <button
            className="boton-secundario"
            type="button"
            disabled={exportando}
            onClick={exportarExcel}
          >
            {exportando ? 'Exportando...' : 'Exportar a Excel'}
          </button>
        </div>
      </form>

      {loading && (
        <p className="ficha-mensaje">Cargando reporte...</p>
      )}

      {error && (
        <p className="error">
          Error: {error.message}
        </p>
      )}

      {errorExportacion && (
        <p className="error">
          {errorExportacion}
        </p>
      )}

      {data?.reporteAsistencia && (
        <div className="reporte-resultado">
          <p className="fecha-corte">
            Datos al{' '}
            {formatearFechaHora(data.reporteAsistencia.fechaCorte)}
          </p>

          {data.reporteAsistencia.grupos.length === 0 ? (
            <p>
              No se encontraron eventos para los
              filtros seleccionados.
            </p>
          ) : (
            data.reporteAsistencia.grupos.map(
              (grupo, index) => (
                <article
                  className="grupo-reporte"
                  key={index}
                >
                  <div className="grupo-reporte-titulo">
                    {grupo.mes && (
                      <h3>{formatearMes(grupo.mes)}</h3>
                    )}

                    {grupo.tipo && (
                      <h3>{etiquetaTipo(grupo.tipo)}</h3>
                    )}
                  </div>

                  <div className="estadisticas">
                    <div className="estadistica">
                      <span>
                        Eventos
                      </span>

                      <strong>
                        {grupo.cantidadDeEventos}
                      </strong>
                    </div>

                    <div className="estadistica">
                      <span>
                        Inscriptos
                      </span>

                      <strong>
                        {grupo.totalInscriptosAcumulados}
                      </strong>
                    </div>

                    <div className="estadistica">
                      <span>
                        Promedio
                      </span>

                      <strong>
                        {grupo.promedioDeAsistencia.toLocaleString('es-AR', {
                          minimumFractionDigits: 2,
                          maximumFractionDigits: 2
                        })}
                      </strong>
                    </div>
                  </div>

                  <div className="eventos-populares">
                    <h4>
                      Eventos más populares
                    </h4>

                    {grupo.eventosMasPopulares.length === 0 ? (
                      <p>
                        No hay inscripciones.
                      </p>
                    ) : (
                      <ol>
                        {grupo.eventosMasPopulares.map(
                          (evento) => (
                            <li key={evento.id}>
                              <span>{evento.titulo}</span>

                              <span className="populares-cantidad">
                                {evento.cantidadInscriptos}
                              </span>
                            </li>
                          )
                        )}
                      </ol>
                    )}
                  </div>
                </article>
              )
            )
          )}
        </div>
      )}
    </section>
  )
}

export default ReporteAsistencia