import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ErrorApi, mensajeDeError, pedir } from '../api/cliente'
import { busquedaDesdeFiltros, filtrosParaGuardar } from '../filtrosEventos'
import FiltrosEventos from '../components/FiltrosEventos'
import ResumenFiltros from '../components/ResumenFiltros'

// Un favorito en modo edición usa los mismos 5 campos que FiltrosEventos,
// pero como strings de formulario (no el "filtros" del contrato, que tiene
// null y curadorId numérico): filtrosParaGuardar hace esa conversión recién
// al enviar, igual que en Eventos.
function filtrosComoStrings(filtros) {
  return {
    desde: filtros.desde ?? '',
    hasta: filtros.hasta ?? '',
    tipo: filtros.tipo ?? '',
    curadorId: filtros.curadorId ? String(filtros.curadorId) : '',
    estado: filtros.estado ?? ''
  }
}

function Favorito({ favorito, curadores, onEliminado, onNoExiste, onActualizado }) {
  const navigate = useNavigate()

  const [editando, setEditando] = useState(false)
  const [nombre, setNombre] = useState(favorito.nombre)
  const [descripcion, setDescripcion] = useState(favorito.descripcion ?? '')
  const [filtros, setFiltros] = useState(() => filtrosComoStrings(favorito.filtros))
  const [guardando, setGuardando] = useState(false)
  const [errorGuardar, setErrorGuardar] = useState(null)
  const [errorAccion, setErrorAccion] = useState(null)

  function aplicar() {
    navigate(`/eventos?${busquedaDesdeFiltros(favorito.filtros)}`)
  }

  function empezarEdicion() {
    setNombre(favorito.nombre)
    setDescripcion(favorito.descripcion ?? '')
    setFiltros(filtrosComoStrings(favorito.filtros))
    setErrorGuardar(null)
    setEditando(true)
  }

  function cambiarFiltro(campo, valor) {
    setFiltros((filtrosActuales) => ({ ...filtrosActuales, [campo]: valor }))
  }

  async function guardar(e) {
    e.preventDefault()
    setGuardando(true)
    setErrorGuardar(null)

    try {
      const favoritoActualizado = await pedir(`/api/filtros-favoritos/${favorito.id}`, {
        metodo: 'PUT',
        cuerpo: {
          nombre: nombre.trim(),
          descripcion: descripcion.trim() || null,
          filtros: filtrosParaGuardar(filtros)
        }
      })
      onActualizado(favoritoActualizado)
      setEditando(false)
    } catch (error) {
      if (error instanceof ErrorApi && error.estado === 404) {
        onNoExiste()
      } else if (error instanceof ErrorApi && error.estado >= 500) {
        setErrorGuardar('El servidor todavía no implementa los filtros favoritos.')
      } else {
        setErrorGuardar(mensajeDeError(error))
      }
    } finally {
      setGuardando(false)
    }
  }

  async function eliminar() {
    if (!window.confirm(`¿Seguro que querés eliminar "${favorito.nombre}"?`)) {
      return
    }

    setErrorAccion(null)

    try {
      await pedir(`/api/filtros-favoritos/${favorito.id}`, { metodo: 'DELETE' })
      onEliminado(favorito.id)
    } catch (error) {
      if (error instanceof ErrorApi && error.estado === 404) {
        onNoExiste()
      } else if (error instanceof ErrorApi && error.estado >= 500) {
        setErrorAccion('El servidor todavía no implementa los filtros favoritos.')
      } else {
        setErrorAccion(mensajeDeError(error))
      }
    }
  }

  if (editando) {
    return (
      <article className="favorito editando">
        <form onSubmit={guardar}>

          <div className="campo">
            <label htmlFor={`nombre-${favorito.id}`}>
              Nombre
            </label>

            <input
              id={`nombre-${favorito.id}`}
              type="text"
              required
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
            />
          </div>

          <div className="campo">
            <label htmlFor={`descripcion-${favorito.id}`}>
              Descripción
            </label>

            <textarea
              id={`descripcion-${favorito.id}`}
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
            />
          </div>

          <FiltrosEventos
            filtros={filtros}
            curadores={curadores}
            onCambiarFiltro={cambiarFiltro}
          />

          {errorGuardar && (
            <p className="error">{errorGuardar}</p>
          )}

          <div className="favorito-acciones">
            <button
              className="boton-generar"
              type="submit"
              disabled={guardando || !nombre.trim()}
            >
              {guardando ? 'Guardando...' : 'Guardar cambios'}
            </button>

            <button
              className="boton-secundario"
              type="button"
              onClick={() => setEditando(false)}
            >
              Cancelar
            </button>
          </div>

        </form>
      </article>
    )
  }

  return (
    <article className="favorito">
      <h3>{favorito.nombre}</h3>

      {favorito.descripcion && (
        <p>{favorito.descripcion}</p>
      )}

      <ResumenFiltros filtros={favorito.filtros} curadores={curadores} />

      {errorAccion && (
        <p className="error">{errorAccion}</p>
      )}

      <div className="favorito-acciones">
        <button className="boton-generar" type="button" onClick={aplicar}>
          Aplicar
        </button>

        <button className="boton-secundario" type="button" onClick={empezarEdicion}>
          Editar
        </button>

        <button className="boton-peligro" type="button" onClick={eliminar}>
          Eliminar
        </button>
      </div>
    </article>
  )
}

function FiltrosFavoritos() {
  const [curadores, setCuradores] = useState([])
  const [favoritos, setFavoritos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [backendNoDisponible, setBackendNoDisponible] = useState(false)
  const [avisoLista, setAvisoLista] = useState(null)
  const [recarga, setRecarga] = useState(0)

  useEffect(() => {
    let cancelado = false

    pedir('/api/usuarios/curadores')
      .then((datos) => {
        if (!cancelado) {
          setCuradores(datos ?? [])
        }
      })
      .catch(() => {
        // Sin curadores el resumen muestra "curador #id"; no rompe la pantalla.
      })

    return () => {
      cancelado = true
    }
  }, [])

  useEffect(() => {
    let cancelado = false

    pedir('/api/filtros-favoritos')
      .then((datos) => {
        if (cancelado) {
          return
        }
        if (!Array.isArray(datos)) {
          setBackendNoDisponible(true)
          setCargando(false)
          return
        }
        setFavoritos(datos)
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
  }, [recarga])

  function alEliminar(id) {
    setFavoritos((favoritosActuales) => favoritosActuales.filter((f) => f.id !== id))
  }

  // Un favorito editado o borrado que ya no existe (404) puede significar que
  // cambió más de lo que se ve acá, así que se recarga toda la lista en vez
  // de sacar el ítem a mano.
  function alNoExistir() {
    setAvisoLista('Ese filtro ya no existe.')
    setCargando(true)
    setRecarga((valor) => valor + 1)
  }

  function alActualizar(favoritoActualizado) {
    setFavoritos((favoritosActuales) =>
      favoritosActuales.map((f) => (f.id === favoritoActualizado.id ? favoritoActualizado : f))
    )
  }

  return (
    <>

      <header className="encabezado">
        <h1>Mis filtros</h1>

        <p>
          Búsquedas de eventos que guardaste para volver a aplicarlas con un clic.
        </p>
      </header>

      {cargando && (
        <p className="ficha-mensaje">Cargando filtros favoritos...</p>
      )}

      {backendNoDisponible && (
        <p className="error">
          El servidor todavía no implementa los filtros favoritos.
        </p>
      )}

      {!cargando && !backendNoDisponible && error && (
        <p className="error">{error}</p>
      )}

      {avisoLista && (
        <p className="error">{avisoLista}</p>
      )}

      {!cargando && !backendNoDisponible && !error && favoritos.length === 0 && (
        <p>
          Todavía no guardaste filtros. Aplicá filtros en{' '}
          <Link to="/eventos">Eventos</Link> y tocá "Guardar estos filtros".
        </p>
      )}

      {!cargando && !backendNoDisponible && !error && favoritos.length > 0 && (
        <div className="favoritos">
          {favoritos.map((favorito) => (
            <Favorito
              key={favorito.id}
              favorito={favorito}
              curadores={curadores}
              onEliminado={alEliminar}
              onNoExiste={alNoExistir}
              onActualizado={alActualizar}
            />
          ))}
        </div>
      )}

    </>
  )
}

export default FiltrosFavoritos
