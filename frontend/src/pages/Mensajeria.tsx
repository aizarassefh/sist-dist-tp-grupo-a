import { useState, type ReactElement } from 'react'
import { mensajeDeError } from '../api/cliente'
import { obtenerConsistencia, obtenerFallidos, reprocesarFallido } from '../api/mensajeria'
import { useCarga } from '../api/useCarga'
import { formatearFechaHora } from '../fechas'
import { ROLES, tieneRol } from '../router/roles'
import type { Usuario } from '../types/modelos'

const NOMBRES_CONSUMIDOR: Record<string, string> = {
  notificaciones: 'Notificaciones',
  auditoria: 'Historial (auditoría)',
}

// Los fallidos pueden traer el contenido como JSON; si se puede, se muestra
// indentado. Si no es JSON válido (justamente un motivo de fallo común) se
// muestra tal cual llegó.
function formatearContenido(contenido: string): string {
  try {
    return JSON.stringify(JSON.parse(contenido), null, 2)
  } catch {
    return contenido
  }
}

function Mensajeria({ usuario }: { usuario: Usuario | null }): ReactElement {
  const [recargas, setRecargas] = useState(0)
  const [reprocesando, setReprocesando] = useState<number | null>(null)
  const [errorAccion, setErrorAccion] = useState<string | null>(null)
  const esAdministrador = tieneRol(usuario, ROLES.ADMINISTRADOR)

  // Las dos consultas van juntas para que el panel y la lista de fallidos
  // siempre muestren el mismo instante.
  const estado = useCarga(String(recargas), () =>
    Promise.all([obtenerConsistencia(), obtenerFallidos()]),
  )
  const consistencia = estado.datos?.[0]
  const fallidos = estado.datos?.[1] ?? []

  async function reprocesar(id: number): Promise<void> {
    setReprocesando(id)
    setErrorAccion(null)
    try {
      await reprocesarFallido(id)
      setRecargas((n) => n + 1)
    } catch (error) {
      setErrorAccion(mensajeDeError(error))
    } finally {
      setReprocesando(null)
    }
  }

  return (
    <section className="mensajeria">
      <header className="encabezado">
        <p className="rotulo">Administración del museo</p>
        <h1>Mensajería</h1>
        <p>
          Estado de los mensajes que se publican al operar con eventos y de lo que
          procesó cada consumidor.
        </p>
      </header>

      <div className="mensajeria-barra">
        <p className="aclaracion-eventual">
          Un desfasaje de pocos segundos es normal: los consumidores procesan de forma asincrónica.
        </p>
        <button
          type="button"
          className="boton-secundario"
          disabled={estado.cargando}
          onClick={() => setRecargas((n) => n + 1)}
        >
          Actualizar
        </button>
      </div>

      {errorAccion && <p className="error" role="alert">{errorAccion}</p>}
      {estado.cargando && <p className="ficha-mensaje" role="status">Cargando estado de la mensajería...</p>}
      {estado.error && <p className="error" role="alert">{mensajeDeError(estado.error)}</p>}

      {consistencia && (
        <>
          <p
            className={`mensajeria-indicador ${consistencia.consistente ? 'mensajeria-ok' : 'mensajeria-pendiente'}`}
            role="status"
          >
            {consistencia.consistente ? 'Consistente' : 'Hay mensajes pendientes'}
          </p>

          <div className="estadisticas mensajeria-cifras">
            <div className="estadistica">
              <span>Generados</span>
              <strong>{consistencia.generados}</strong>
            </div>
            <div className="estadistica">
              <span>Publicados</span>
              <strong>{consistencia.publicados}</strong>
            </div>
            <div className="estadistica">
              <span>Pendientes de publicar</span>
              <strong>{consistencia.pendientesDePublicar}</strong>
            </div>
            <div className="estadistica">
              <span>Fallidos pendientes</span>
              <strong>{consistencia.fallidosPendientes}</strong>
            </div>
          </div>

          <h2 className="mensajeria-subtitulo">Por consumidor</h2>
          <div className="historial-tabla-contenedor">
            <table className="historial-tabla">
              <thead>
                <tr>
                  <th>Consumidor</th>
                  <th>Procesados</th>
                  <th>Faltantes</th>
                </tr>
              </thead>
              <tbody>
                {consistencia.consumidores.map((consumidor) => (
                  <tr key={consumidor.consumidor}>
                    <td>{NOMBRES_CONSUMIDOR[consumidor.consumidor] ?? consumidor.consumidor}</td>
                    <td>{consumidor.procesados}</td>
                    <td>
                      {consumidor.faltantes.length === 0 ? (
                        '0'
                      ) : (
                        <details className="historial-detalle">
                          <summary>{consumidor.faltantes.length} sin procesar</summary>
                          <ul className="mensajeria-ids">
                            {consumidor.faltantes.map((idMensaje) => (
                              <li key={idMensaje}><code>{idMensaje}</code></li>
                            ))}
                          </ul>
                        </details>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <h2 className="mensajeria-subtitulo">Mensajes fallidos</h2>
          {fallidos.length === 0 && <p className="ficha-vacio">No hay mensajes fallidos pendientes.</p>}
          {fallidos.length > 0 && (
            <ul className="mensajeria-fallidos">
              {fallidos.map((fallido) => (
                <li key={fallido.id} className="mensajeria-fallido">
                  <div className="notificacion-cabecera">
                    <span className="notificacion-tipo">
                      {NOMBRES_CONSUMIDOR[fallido.consumidor] ?? fallido.consumidor}
                    </span>
                    <time className="notificacion-fecha">{formatearFechaHora(fallido.fechaRecepcion)}</time>
                  </div>
                  <p className="mensajeria-error">{fallido.error}</p>
                  {fallido.idMensaje && <p className="mensajeria-id"><code>{fallido.idMensaje}</code></p>}
                  <details className="historial-detalle">
                    <summary>Ver contenido</summary>
                    <pre className="mensajeria-contenido">{formatearContenido(fallido.contenido)}</pre>
                  </details>
                  {esAdministrador && (
                    <button
                      type="button"
                      disabled={reprocesando !== null}
                      onClick={() => reprocesar(fallido.id)}
                    >
                      {reprocesando === fallido.id ? 'Reprocesando...' : 'Reprocesar'}
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  )
}

export default Mensajeria
