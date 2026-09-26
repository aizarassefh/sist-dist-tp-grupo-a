import { useState } from 'react'
import { useQuery } from '@apollo/client/react'
import { REPORTE_ASISTENCIA_QUERY } from '../graphql/queries'

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

  return (
    <section className="reporte">
      <div className="reporte-encabezado">
        <h2>Reporte de asistencia</h2>

        <p>
          Consulta la cantidad de eventos e inscripciones
          registrados.
        </p>
      </div>

      <form
        className="reporte-filtros"
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

          <input
            id="tipo"
            type="text"
            value={filtros.tipo}
            onChange={(e) =>
              cambiarFiltro('tipo', e.target.value)
            }
            placeholder="Ej. Taller"
          />
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

        <button
          className="boton-generar"
          type="submit"
        >
          Generar reporte
        </button>
      </form>

      {loading && (
        <p>Cargando reporte...</p>
      )}

      {error && (
        <p className="error">
          Error: {error.message}
        </p>
      )}

      {data?.reporteAsistencia && (
        <div className="reporte-resultado">
          <p className="fecha-corte">
            Fecha de corte:{' '}
            {data.reporteAsistencia.fechaCorte}
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
                      <h3>{grupo.mes}</h3>
                    )}

                    {grupo.tipo && (
                      <h3>{grupo.tipo}</h3>
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
                        {grupo.promedioDeAsistencia.toFixed(2)}
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
                              <strong>
                                {evento.titulo}
                              </strong>

                              {' — '}

                              {evento.cantidadInscriptos}{' '}
                              inscriptos
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