import { useState, type ReactElement } from 'react'
import { Link } from 'react-router-dom'
import { mensajeDeError } from '../api/cliente'
import { marcarNotificacionLeida, marcarTodasLeidas, obtenerNotificaciones } from '../api/mensajeria'
import { useCarga } from '../api/useCarga'
import { formatearFechaHora } from '../fechas'
import type { Notificacion, TipoNovedad } from '../types/modelos'

const ETIQUETAS_TIPO: Record<TipoNovedad, string> = {
  ALTA: 'Nuevo evento',
  MODIFICACION: 'Evento modificado',
  CANCELACION: 'Evento cancelado',
}

function Notificaciones(): ReactElement {
  const [soloNoLeidas, setSoloNoLeidas] = useState(false)
  // Contador que cambia la clave de useCarga para volver a pedir la lista.
  const [recargas, setRecargas] = useState(0)
  const [errorAccion, setErrorAccion] = useState<string | null>(null)

  const lista = useCarga(`${soloNoLeidas}|${recargas}`, () => obtenerNotificaciones(soloNoLeidas))
  const notificaciones: Notificacion[] = lista.datos?.notificaciones ?? []
  const noLeidas = lista.datos?.noLeidas ?? 0

  async function marcarLeida(notificacion: Notificacion): Promise<void> {
    if (notificacion.leida) return
    try {
      await marcarNotificacionLeida(notificacion.id)
      setRecargas((n) => n + 1)
    } catch (error) {
      setErrorAccion(mensajeDeError(error))
    }
  }

  async function marcarTodas(): Promise<void> {
    setErrorAccion(null)
    try {
      await marcarTodasLeidas()
      setRecargas((n) => n + 1)
    } catch (error) {
      setErrorAccion(mensajeDeError(error))
    }
  }

  return (
    <>
      <header className="encabezado">
        <p className="rotulo">Mis avisos</p>
        <h1>Notificaciones</h1>
        <p>Novedades de los eventos del museo que te interesan o en los que estás inscripto.</p>
      </header>

      <p className="aclaracion-eventual">
        Las notificaciones pueden tardar unos segundos en aparecer después de que un evento
        cambia: se generan de forma asincrónica.
      </p>

      <div className="notificaciones-barra">
        <label className="notificaciones-filtro">
          <input
            type="checkbox"
            checked={soloNoLeidas}
            onChange={(e) => setSoloNoLeidas(e.target.checked)}
          />
          Solo no leídas
        </label>
        <button
          type="button"
          className="boton-secundario"
          disabled={noLeidas === 0}
          onClick={marcarTodas}
        >
          Marcar todas como leídas
        </button>
      </div>

      {errorAccion && <p className="error" role="alert">{errorAccion}</p>}
      {lista.cargando && <p className="ficha-mensaje" role="status">Cargando notificaciones...</p>}
      {lista.error && <p className="error" role="alert">{mensajeDeError(lista.error)}</p>}
      {lista.datos && notificaciones.length === 0 && (
        <p className="ficha-vacio">
          {soloNoLeidas ? 'No tenés notificaciones sin leer.' : 'Todavía no tenés notificaciones.'}
        </p>
      )}

      {notificaciones.length > 0 && (
        <ul className="notificaciones-lista">
          {notificaciones.map((n) => (
            <li key={n.id} className={`notificacion${n.leida ? '' : ' notificacion-nueva'}`}>
              <div className="notificacion-cabecera">
                <span className="notificacion-tipo">{ETIQUETAS_TIPO[n.tipo] ?? n.tipo}</span>
                <time className="notificacion-fecha">{formatearFechaHora(n.fechaOperacion)}</time>
              </div>

              <h2 className="notificacion-evento">
                {/* Un evento cancelado ya no existe: el link llevaría a un 404. */}
                {n.tipo === 'CANCELACION' ? (
                  n.tituloEvento
                ) : (
                  <Link to={`/eventos/${n.idEvento}`} onClick={() => marcarLeida(n)}>
                    {n.tituloEvento}
                  </Link>
                )}
              </h2>

              <p className="notificacion-mensaje">{n.mensaje}</p>

              {!n.leida && (
                <button type="button" className="boton-secundario" onClick={() => marcarLeida(n)}>
                  Marcar como leída
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </>
  )
}

export default Notificaciones
