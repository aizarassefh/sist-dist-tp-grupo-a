import { useState } from 'react'
import { useQuery } from '@apollo/client/react'
import { OBRAS_LISTADO_QUERY } from '../graphql/queries'
import Obra from '../components/Obra'
import FiltrosObras from '../components/FiltrosObras'

const TAMANIO_PAGINA = 6

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
    OBRAS_LISTADO_QUERY,
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

export default Catalogo
