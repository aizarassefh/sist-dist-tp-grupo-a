import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ErrorApi, mensajeDeError, pedir } from '../api/cliente'
import { esGestor } from '../roles'
import { CAMPOS_FILTRO_EVENTOS, filtrosDesdeBusqueda, filtrosParaGuardar } from '../filtrosEventos'
import FiltrosEventos from '../components/FiltrosEventos'
import ResumenFiltros from '../components/ResumenFiltros'
import AgendaEventos from '../components/AgendaEventos'

const TAMANIO_PAGINA = 10

// Los filtros y la página viven en la URL (no en useState) para que se
// conserven al recargar, al volver del detalle, y para que más adelante un
// filtro favorito se "aplique" navegando a esta misma ruta con sus filtros.
function Eventos({ usuario }) {
  const [searchParams, setSearchParams] = useSearchParams()

  const filtrosActuales = filtrosDesdeBusqueda(searchParams)
  const { desde, hasta, tipo, curadorId, estado } = filtrosActuales
  const paginaActual = Number(searchParams.get('pagina')) || 1

  const [curadores, setCuradores] = useState([])
  const [eventos, setEventos] = useState([])
  const [total, setTotal] = useState(0)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [backendNoDisponible, setBackendNoDisponible] = useState(false)

  const [mostrarGuardarFiltro, setMostrarGuardarFiltro] = useState(false)
  const [nombreFavorito, setNombreFavorito] = useState('')
  const [descripcionFavorito, setDescripcionFavorito] = useState('')
  const [guardandoFavorito, setGuardandoFavorito] = useState(false)
  const [errorGuardarFavorito, setErrorGuardarFavorito] = useState(null)
  const [favoritoGuardado, setFavoritoGuardado] = useState(false)

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

  function alternarFormularioGuardar() {
    setFavoritoGuardado(false)
    setErrorGuardarFavorito(null)
    setMostrarGuardarFiltro((valor) => !valor)
  }

  async function guardarFiltro(e) {
    e.preventDefault()
    setGuardandoFavorito(true)
    setErrorGuardarFavorito(null)

    try {
      await pedir('/api/filtros-favoritos', {
        metodo: 'POST',
        cuerpo: {
          nombre: nombreFavorito.trim(),
          descripcion: descripcionFavorito.trim() || null,
          filtros: filtrosParaGuardar(filtrosActuales)
        }
      })
      setMostrarGuardarFiltro(false)
      setFavoritoGuardado(true)
      setNombreFavorito('')
      setDescripcionFavorito('')
    } catch (error) {
      // Mismo criterio que el resto de la pantalla: un 500 acá significa que
      // el backend todavía no tiene filtros favoritos, no un error real.
      if (error instanceof ErrorApi && error.estado >= 500) {
        setErrorGuardarFavorito('El servidor todavía no implementa los filtros favoritos.')
      } else {
        setErrorGuardarFavorito(mensajeDeError(error))
      }
    } finally {
      setGuardandoFavorito(false)
    }
  }

  const hayFiltroAplicado = CAMPOS_FILTRO_EVENTOS.some((campo) => filtrosActuales[campo])
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

      <section className="eventos">

        <div className="eventos-encabezado">
          <p className="rotulo eventos-total">
            {cargando || backendNoDisponible ? 'Agenda' : `${total} ${total === 1 ? 'evento' : 'eventos'}`}
          </p>

          <div className="eventos-encabezado-acciones">
            <button
              className="boton-generar"
              type="button"
              disabled={!hayFiltroAplicado}
              // Como tooltip y no como texto fijo: sin filtros es el estado
              // normal del listado y el aviso quedaría siempre a la vista.
              title={hayFiltroAplicado ? undefined : 'Aplicá al menos un filtro para guardarlo.'}
              onClick={alternarFormularioGuardar}
            >
              Guardar estos filtros
            </button>

            {esGestor(usuario) && (
              <Link className="boton-generar" to="/eventos/nuevo">
                Nuevo evento
              </Link>
            )}
          </div>
        </div>

        {mostrarGuardarFiltro && (
          <form className="guardar-filtro" onSubmit={guardarFiltro}>

            <div className="campo">
              <label htmlFor="nombreFavorito">
                Nombre
              </label>

              <input
                id="nombreFavorito"
                type="text"
                required
                value={nombreFavorito}
                onChange={(e) => setNombreFavorito(e.target.value)}
              />
            </div>

            <div className="campo">
              <label htmlFor="descripcionFavorito">
                Descripción
              </label>

              <textarea
                id="descripcionFavorito"
                value={descripcionFavorito}
                onChange={(e) => setDescripcionFavorito(e.target.value)}
              />
            </div>

            <ResumenFiltros
              filtros={filtrosParaGuardar(filtrosActuales)}
              curadores={curadores}
            />

            {errorGuardarFavorito && (
              <p className="error">{errorGuardarFavorito}</p>
            )}

            <div className="guardar-filtro-acciones">
              <button
                className="boton-generar"
                type="submit"
                disabled={guardandoFavorito || !nombreFavorito.trim()}
              >
                {guardandoFavorito ? 'Guardando...' : 'Guardar'}
              </button>

              <button
                className="boton-secundario"
                type="button"
                onClick={() => setMostrarGuardarFiltro(false)}
              >
                Cancelar
              </button>
            </div>

          </form>
        )}

        {favoritoGuardado && (
          <p>
            Filtro guardado.{' '}
            <Link to="/favoritos">Ver mis filtros favoritos</Link>
          </p>
        )}

        {cargando && (
          <p className="ficha-mensaje">Cargando eventos...</p>
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
            <AgendaEventos
              eventos={eventos}
              volverA={`/eventos${searchParams.toString() ? `?${searchParams}` : ''}`}
            />

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
