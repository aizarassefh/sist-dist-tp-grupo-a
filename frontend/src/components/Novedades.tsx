import { useEffect, useRef, useState, type ReactElement } from 'react'
import { useNavigate } from 'react-router-dom'
import { mensajeDeError, pedir } from '../api/cliente'
import { esNovedad, type Novedad, type TipoNovedad } from '../types/modelos'
import { NOVEDADES_SUBSCRIPTION } from '../graphql/queries'
import { useSubscription } from '@apollo/client/react'

const INTERVALO_ACTUALIZACION = 30_000
const CLAVE_NOVEDADES_OCULTAS = 'museo:novedades-ocultas'

const etiquetasTipo: Record<TipoNovedad, string> = {
  ALTA: 'Nuevo evento',
  MODIFICACION: 'Evento actualizado',
  CANCELACION: 'Evento cancelado',
}

function leerNovedadesOcultas(): Set<string> {
  try {
    const guardadas: unknown = JSON.parse(localStorage.getItem(CLAVE_NOVEDADES_OCULTAS) ?? '[]')
    return new Set(Array.isArray(guardadas) ? guardadas.filter((id): id is string => typeof id === 'string') : [])
  } catch {
    return new Set()
  }
}

function fechaNovedad(novedad: Novedad): string {
  const fecha = novedad.occurredAt ?? novedad.fechaHora
  if (!fecha) {
    return ''
  }

  const fechaFormateada = new Date(fecha)
  if (Number.isNaN(fechaFormateada.getTime())) {
    return ''
  }

  return new Intl.DateTimeFormat('es-AR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(fechaFormateada)
}

function Novedades(): ReactElement {
  const navigate = useNavigate()
  const contenedorRef = useRef<HTMLDivElement | null>(null)
  const [novedades, setNovedades] = useState<Novedad[]>([])
  const [cargando, setCargando] = useState<boolean>(true)
  const [error, setError] = useState<string>('')
  const [abierto, setAbierto] = useState<boolean>(false)
  const [ocultas, setOcultas] = useState<Set<string>>(() => leerNovedadesOcultas())

  function guardarOcultas(nuevasOcultas: Set<string>): void {
    setOcultas(nuevasOcultas)
    localStorage.setItem(CLAVE_NOVEDADES_OCULTAS, JSON.stringify([...nuevasOcultas]))
  }

  function quitarNovedad(novedad: Novedad): void {
    setNovedades((actuales) => actuales.filter((item) => item.messageId !== novedad.messageId))
    guardarOcultas(new Set([...ocultas, novedad.messageId]))
  }
  useSubscription<{ novedades: Novedad }>(NOVEDADES_SUBSCRIPTION, {
    onData: ({ data }) => {
      const novedad = data.data?.novedades
      if (!novedad || !esNovedad(novedad)) return
      if (ocultas.has(novedad.messageId)) return
      setNovedades((actuales) => [novedad, ...actuales.filter((item) => item.messageId !== novedad.messageId)].slice(0, 100))
      setError('')
      setCargando(false)
    },
  })

  useEffect(() => {
    let cancelado = false

    async function cargarNovedades() {
      try {
        const datos = await pedir('/api/novedades')
        if (!cancelado) {
          const novedadesValidas = Array.isArray(datos)
            ? datos.filter(esNovedad).filter((novedad) => !ocultas.has(novedad.messageId))
            : []
          setNovedades(novedadesValidas)
          setError('')
          setCargando(false)
        }
      } catch (errorApi) {
        if (!cancelado) {
          setError(mensajeDeError(errorApi))
          setCargando(false)
        }
      }
    }

    cargarNovedades()
    const intervalo = window.setInterval(cargarNovedades, INTERVALO_ACTUALIZACION)

    return () => {
      cancelado = true
      window.clearInterval(intervalo)
    }
  }, [ocultas])

  useEffect(() => {
    function cerrarConEscape(evento: globalThis.KeyboardEvent): void {
      if (evento.key === 'Escape') {
        setAbierto(false)
      }
    }

    function cerrarAlHacerClickAfuera(evento: globalThis.MouseEvent): void {
      if (contenedorRef.current && evento.target instanceof Node && !contenedorRef.current.contains(evento.target)) {
        setAbierto(false)
      }
    }

    document.addEventListener('keydown', cerrarConEscape)
    document.addEventListener('mousedown', cerrarAlHacerClickAfuera)
    return () => {
      document.removeEventListener('keydown', cerrarConEscape)
      document.removeEventListener('mousedown', cerrarAlHacerClickAfuera)
    }
  }, [])

  function abrirEvento(evento: Novedad): void {
    quitarNovedad(evento)
    setAbierto(false)
    if (evento.type !== 'CANCELACION') {
      navigate(`/eventos/${evento.eventId}`)
    }
  }

  function limpiarNovedades(): void {
    guardarOcultas(new Set([...ocultas, ...novedades.map((novedad) => novedad.messageId)]))
    setNovedades([])
  }

  return (
    <div className="novedades" ref={contenedorRef}>
      <button
        type="button"
        className="novedades-boton"
        aria-expanded={abierto}
        aria-controls="novedades-menu"
        aria-label={`Novedades${novedades.length ? `, ${novedades.length} disponibles` : ''}`}
        onClick={() => setAbierto((estado) => !estado)}
      >
        <span aria-hidden="true" className="novedades-icono">♢</span>
        <span>Novedades</span>
        {novedades.length > 0 && <span className="novedades-contador">{novedades.length}</span>}
      </button>

      {abierto && (
        <section className="novedades-menu" id="novedades-menu" aria-label="Novedades recientes">
            <div className="novedades-encabezado">
              <h2>Novedades</h2>
              <div className="novedades-acciones">
                <span>{novedades.length} {novedades.length === 1 ? 'aviso' : 'avisos'}</span>
                {novedades.length > 0 && (
                  <button type="button" className="novedades-limpiar" onClick={limpiarNovedades}>
                    Limpiar todas
                  </button>
                )}
              </div>
          </div>

          {cargando && <p className="novedades-estado">Cargando novedades...</p>}
          {!cargando && error && <p className="novedades-estado novedades-error">{error}</p>}
          {!cargando && !error && novedades.length === 0 && (
            <p className="novedades-estado">No hay novedades por ahora.</p>
          )}
          {!cargando && !error && novedades.length > 0 && (
            <ul className="novedades-lista">
              {novedades.slice(0, 5).map((novedad) => (
                <li key={novedad.messageId}>
                  <button
                    type="button"
                    className={`novedad-item${novedad.type === 'CANCELACION' ? ' novedad-cancelada' : ''}`}
                    onClick={() => abrirEvento(novedad)}
                  >
                    <strong>{etiquetasTipo[novedad.type] ?? 'Actualización'}</strong>
                    <span>{novedad.title}</span>
                    <small>{fechaNovedad(novedad)}{novedad.type === 'CANCELACION' ? ' · Sin evento disponible' : ''}</small>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </div>
  )
}

export default Novedades
