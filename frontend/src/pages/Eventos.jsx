import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ErrorApi, mensajeDeError, pedir } from '../api/cliente'
import { esGestor } from '../roles'
import { etiquetaTipo } from '../tiposEvento'
import { formatearFechaHora, yaComenzo } from '../fechas'
import FiltrosEventos from '../components/FiltrosEventos'

const TAMANIO_PAGINA = 10

function TarjetaEvento({ evento, busqueda }) {
  const comenzo = yaComenzo(evento.fechaHora)
  const completo = evento.cantidadInscriptos >= evento.cupoMaximo

  return (
    <article className="obra">

      <h2>{evento.titulo}</h2>

      <p className="obra-artista">
        {etiquetaTipo(evento.tipo)}
      </p>

      <p>{formatearFechaHora(evento.fechaHora)}</p>

      <p>{evento.duracionMinutos} minutos</p>

      <p>{evento.curadorResponsable.nombre}</p>

      <p>
        {evento.cantidadInscriptos} / {evento.cupoMaximo} inscriptos
      </p>

      <div className="etiquetas-evento">
        {evento.inscripto && (
          <span className="estado inscripto">Inscripto</span>
        )}
        {comenzo && (
          <span className="estado finalizado">Finalizado</span>
        )}
        {completo && (
          <span className="estado completo">Completo</span>
        )}
      </div>

      {/* El detalle recibe a qué listado volver, con los filtros actuales. */}
      <Link
        className="boton-detalle"
        to={`/eventos/${evento.id}`}
        state={{ volverA: `/eventos${busqueda ? `?${busqueda}` : ''}` }}
      >
        Ver detalle
      </Link>

    </article>
  )
}

// Los filtros y la página viven en la URL (no en useState) para que se
// conserven al recargar, al volver del detalle, y para que más adelante un
// filtro favorito se "aplique" navegando a esta misma ruta con sus filtros.
function Eventos({ usuario }) {
  const [searchParams, setSearchParams] = useSearchParams()

  const desde = searchParams.get('desde') ?? ''
  const hasta = searchParams.get('hasta') ?? ''
  const tipo = searchParams.get('tipo') ?? ''
  const curadorId = searchParams.get('curadorId') ?? ''
  const estado = searchParams.get('estado') ?? ''
  const paginaActual = Number(searchParams.get('pagina')) || 1

  const [curadores, setCuradores] = useState([])
  const [eventos, setEventos] = useState([])
  const [total, setTotal] = useState(0)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [backendNoDisponible, setBackendNoDisponible] = useState(false)

  useEffect(() => {
    let cancelado = false

    pedir('/api/usuarios/curadores')
      .then((datos) => {
        if (!cancelado) {
          setCuradores(datos ?? [])
        }
      })
      .catch(() => {
        // Sin curadores el filtro queda solo con "Todos"; no rompe la pantalla.
      })

    return () => {
      cancelado = true
    }
  }, [])

  useEffect(() => {
    let cancelado = false

    pedir('/api/eventos', {
      parametros: {
        desde,
        hasta,
        tipo,
        curadorId,
        estado,
        pagina: paginaActual - 1,
        tamanio: TAMANIO_PAGINA
      }
    })
      .then((datos) => {
        if (cancelado) {
          return
        }
        // El backend viejo devuelve un array plano en vez de la página con
        // "items": ahí todavía no está implementado el contrato nuevo.
        if (!datos || !Array.isArray(datos.items)) {
          setBackendNoDisponible(true)
          setCargando(false)
          return
        }
        setEventos(datos.items)
        setTotal(datos.total)
        setError(null)
        setBackendNoDisponible(false)
        setCargando(false)
      })
      .catch((error) => {
        if (cancelado) {
          return
        }
        if (error instanceof ErrorApi && error.estado >= 500) {
          setBackendNoDisponible(true)
          setError(null)
        } else {
          setError(mensajeDeError(error))
          setBackendNoDisponible(false)
        }
        setCargando(false)
      })

    return () => {
      cancelado = true
    }
  }, [desde, hasta, tipo, curadorId, estado, paginaActual])

  function cambiarFiltro(nombre, valor) {
    const params = new URLSearchParams(searchParams)

    if (valor) {
      params.set(nombre, valor)
    } else {
      params.delete(nombre)
    }
    // Al cambiar un filtro la página vuelve a 0 (a la página 1 de la URL).
    params.delete('pagina')

    setSearchParams(params)
  }

  function irAPagina(numero) {
    const params = new URLSearchParams(searchParams)

    if (numero <= 1) {
      params.delete('pagina')
    } else {
      params.set('pagina', String(numero))
    }

    setSearchParams(params)
  }

  const totalPaginas = Math.max(1, Math.ceil(total / TAMANIO_PAGINA))

  return (
    <>

      <header className="encabezado">
        <h1>Eventos</h1>

        <p>
          Visitas guiadas, talleres y charlas del museo
        </p>
      </header>

      <FiltrosEventos
        filtros={{ desde, hasta, tipo, curadorId, estado }}
        curadores={curadores}
        onCambiarFiltro={cambiarFiltro}
      />

      <section className="catalogo">

        <div className="eventos-encabezado">
          <h2>Eventos</h2>

          {esGestor(usuario) && (
            <Link className="boton-generar" to="/eventos/nuevo">
              Nuevo evento
            </Link>
          )}
        </div>

        {cargando && (
          <p>Cargando eventos...</p>
        )}

        {backendNoDisponible && (
          <p className="error">
            El servidor todavía no implementa la gestión de eventos.
          </p>
        )}

        {!cargando && !backendNoDisponible && error && (
          <p className="error">{error}</p>
        )}

        {!cargando && !backendNoDisponible && !error && eventos.length === 0 && (
          <p>No hay eventos para esos filtros.</p>
        )}

        {!cargando && !backendNoDisponible && !error && eventos.length > 0 && (
          <>
            <div className="obras">
              {eventos.map((evento) => (
                <TarjetaEvento
                  key={evento.id}
                  evento={evento}
                  busqueda={searchParams.toString()}
                />
              ))}
            </div>

            <div className="paginacion">
              <button
                type="button"
                onClick={() => irAPagina(paginaActual - 1)}
                disabled={paginaActual <= 1}
              >
                ← Anterior
              </button>

              <span>
                Página {paginaActual} de {totalPaginas}
              </span>

              <button
                type="button"
                onClick={() => irAPagina(paginaActual + 1)}
                disabled={paginaActual >= totalPaginas}
              >
                Siguiente →
              </button>
            </div>
          </>
        )}

      </section>

    </>
  )
}

export default Eventos
