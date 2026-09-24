import { useQuery } from '@apollo/client/react'
import {
  Link,
  Route,
  Routes,
  useNavigate,
  useParams
} from 'react-router-dom'
import { useState } from 'react'
import { OBRAS_QUERY } from './graphql/queries'
import Obra from './components/Obra'
import FiltrosObras from './components/FiltrosObras'
import ReporteAsistencia from './components/ReporteAsistencia'
import Home from './components/Home'

const TAMANIO_PAGINA = 2

function Navegacion() {
  return (
    <nav className="navegacion">

      <Link
        to="/"
        className="navegacion-logo"
      >
        Museo Virtual
      </Link>

      <div className="navegacion-links">

        <Link
          to="/"
          className="navegacion-link"
        >
          Inicio
        </Link>

        <Link
          to="/coleccion"
          className="navegacion-link"
        >
          Colección
        </Link>

        <Link
          to="/reporte"
          className="navegacion-link"
        >
          Reporte
        </Link>

      </div>

    </nav>
  )
}

function Inicio() {
  const navigate = useNavigate()

  return (
    <Home
      onExplorar={() => navigate('/coleccion')}
    />
  )
}

function Catalogo() {
  const [filtros, setFiltros] = useState({
    palabraClave: '',
    epoca: '',
    tecnica: '',
    ubicacion: '',
    enExhibicion: null
  })

  const [pagina, setPagina] = useState(0)

  const { loading, error, data } = useQuery(
    OBRAS_QUERY,
    {
      variables: {
        filtro: {
          palabraClave: filtros.palabraClave || null,
          epoca: filtros.epoca || null,
          tecnica: filtros.tecnica || null,
          ubicacion: filtros.ubicacion || null,
          enExhibicion: filtros.enExhibicion
        },
        pagina: pagina,
        tamanio: TAMANIO_PAGINA
      }
    }
  )

  function cambiarFiltro(nombre, valor) {
    setFiltros((filtrosActuales) => ({
      ...filtrosActuales,
      [nombre]: valor
    }))

    setPagina(0)
  }

  function paginaAnterior() {
    if (pagina > 0) {
      setPagina(pagina - 1)
    }
  }

  function paginaSiguiente() {
    if (data?.obras?.length === TAMANIO_PAGINA) {
      setPagina(pagina + 1)
    }
  }

  return (
    <>

      <header className="encabezado">

        <h1>Museo Virtual</h1>

        <p>
          Explorá nuestra colección de obras de arte
        </p>

      </header>

      <FiltrosObras
        filtros={filtros}
        onCambiarFiltro={cambiarFiltro}
      />

      <section className="catalogo">

        <h2>Obras de la colección</h2>

        {loading && (
          <p>Cargando obras...</p>
        )}

        {error && (
          <p className="error">
            Error: {error.message}
          </p>
        )}

        {!loading &&
          !error &&
          data?.obras?.length === 0 && (

            <p>
              No se encontraron obras para los
              filtros seleccionados.
            </p>

          )}

        <div className="obras">

          {data?.obras?.map((obra) => (

            <Obra
              key={obra.id}
              obra={obra}
            />

          ))}

        </div>

        {!loading &&
          !error &&
          data?.obras?.length > 0 && (

            <div className="paginacion">

              <button
                type="button"
                onClick={paginaAnterior}
                disabled={pagina === 0}
              >
                ← Anterior
              </button>

              <span>
                Página {pagina + 1}
              </span>

              <button
                type="button"
                onClick={paginaSiguiente}
                disabled={
                  data.obras.length < TAMANIO_PAGINA
                }
              >
                Siguiente →
              </button>

            </div>

          )}

      </section>

    </>
  )
}

function DetalleObra() {
  const { id } = useParams()

  const { loading, error, data } = useQuery(
    OBRAS_QUERY,
    {
      variables: {
        filtro: null,
        pagina: 0,
        tamanio: 100
      }
    }
  )

  if (loading) {
    return <p>Cargando obra...</p>
  }

  if (error) {
    return (
      <p className="error">
        Error: {error.message}
      </p>
    )
  }

  const obra = data?.obras?.find(
    (obraActual) => String(obraActual.id) === String(id)
  )

  if (!obra) {
    return (
      <section className="detalle-obra">

        <h2>Obra no encontrada</h2>

        <p>
          No existe una obra con el identificador {id}.
        </p>

        <Link
          className="boton-volver"
          to="/coleccion"
        >
          ← Volver a la colección
        </Link>

      </section>
    )
  }

  return (
    <section className="detalle-obra">

      <Link
        className="boton-volver"
        to="/coleccion"
      >
        ← Volver a la colección
      </Link>

      <div className="detalle-contenido">

        <div>

          {obra.imagenUrl ? (

            <img
              className="detalle-imagen"
              src={obra.imagenUrl}
              alt={`Obra ${obra.titulo}`}
            />

          ) : (

            <div className="obra-sin-imagen">
              Sin imagen disponible
            </div>

          )}

        </div>

        <div className="detalle-informacion">

          <h2>
            {obra.titulo}
          </h2>

          <p className="obra-artista">
            {obra.artista.nombre}
          </p>

          <p>
            <strong>Año:</strong>{' '}
            {obra.anioCreacion}
          </p>

          <p>
            <strong>Técnica:</strong>{' '}
            {obra.tecnica}
          </p>

          <p>
            <strong>Dimensiones:</strong>{' '}
            {obra.dimensiones}
          </p>

          <p>
            <strong>Época:</strong>{' '}
            {obra.epoca}
          </p>

          <p>
            <strong>Ubicación:</strong>{' '}
            {obra.ubicacion}
          </p>

          <span
            className={
              obra.enExhibicion
                ? 'estado exhibicion'
                : 'estado deposito'
            }
          >
            {obra.enExhibicion
              ? 'En exhibición'
              : 'En depósito'}
          </span>

          <h3>Descripción</h3>

          <p className="detalle-descripcion">
            {obra.descripcion}
          </p>

          <h3>Sobre el artista</h3>

          <p className="detalle-descripcion">
            {obra.artista.biografia}
          </p>

          <div className="detalle-comentarios">

            <h3>Comentarios</h3>

            {obra.comentarios.length === 0 ? (

              <p className="sin-comentarios">
                No hay comentarios todavía.
              </p>

            ) : (

              obra.comentarios.map(
                (comentario, index) => (

                  <div
                    className="comentario"
                    key={index}
                  >

                    <p>
                      <strong>
                        {comentario.usuario}
                      </strong>
                    </p>

                    <p>
                      {comentario.texto}
                    </p>

                    <small>
                      {comentario.fecha}
                    </small>

                  </div>

                )
              )

            )}

          </div>

        </div>

      </div>

    </section>
  )
}

function Reporte() {
  return <ReporteAsistencia />
}

function App() {
  return (
    <main className="app">

      <Navegacion />

	  <Routes>

	    <Route
	      path="/"
	      element={<Inicio />}
	    />

	    <Route
	      path="/coleccion"
	      element={<Catalogo />}
	    />

	    <Route
	      path="/obra/:id"
	      element={<DetalleObra />}
	    />

	    <Route
	      path="/reporte"
	      element={<Reporte />}
	    />

	  </Routes>

    </main>
  )
}

export default App