import { useEffect, useState } from 'react'
import { useQuery } from '@apollo/client/react'
import { OBRAS_LISTADO_QUERY } from '../graphql/queries'
import Obra from '../components/Obra'
import FiltrosObras from '../components/FiltrosObras'

const TAMANIO_PAGINA = 6
const ESPERA_ESCRITURA_MS = 300

function Catalogo() {
  const [filtros, setFiltros] = useState({
    palabraClave: '',
    epoca: '',
    tecnica: '',
    ubicacion: '',
    enExhibicion: null
  })

  // Los inputs se actualizan al instante con "filtros"; la consulta usa
  // "filtrosConsulta", que se actualiza cuando el usuario deja de escribir.
  // Sin esto se mandaba una consulta por cada letra.
  const [filtrosConsulta, setFiltrosConsulta] = useState(filtros)

  const [pagina, setPagina] = useState(0)

  useEffect(() => {
    const temporizador = setTimeout(
      () => setFiltrosConsulta(filtros),
      ESPERA_ESCRITURA_MS
    )

    return () => clearTimeout(temporizador)
  }, [filtros])

  const { loading, error, data, previousData } = useQuery(
    OBRAS_LISTADO_QUERY,
    {
      variables: {
        filtro: {
          palabraClave: filtrosConsulta.palabraClave || null,
          epoca: filtrosConsulta.epoca || null,
          tecnica: filtrosConsulta.tecnica || null,
          ubicacion: filtrosConsulta.ubicacion || null,
          enExhibicion: filtrosConsulta.enExhibicion
        },
        pagina: pagina,
        tamanio: TAMANIO_PAGINA
      }
    }
  )

  // Mientras llega el resultado nuevo se siguen mostrando las obras
  // anteriores: si la grilla se vaciara, la página se achica y salta.
  const obras = (data ?? previousData)?.obras ?? []
  const primeraCarga = loading && !data && !previousData

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
    if (obras.length === TAMANIO_PAGINA) {
      setPagina(pagina + 1)
    }
  }

  return (
    <>

      <header className="encabezado">

        <h1>La colección</h1>

        <p>
          Pinturas del Renacimiento al siglo XX. Buscá por título,
          artista, época, técnica o sala.
        </p>

      </header>

      <FiltrosObras
        filtros={filtros}
        onCambiarFiltro={cambiarFiltro}
      />

      <section className="catalogo">

        {primeraCarga && (
          <p>Cargando obras...</p>
        )}

        {error && (
          <p className="error">
            Error: {error.message}
          </p>
        )}

        {!loading &&
          !error &&
          obras.length === 0 && (

            <p>
              No se encontraron obras para los
              filtros seleccionados.
            </p>

          )}

        <div
          className={loading ? 'galeria actualizando' : 'galeria'}
          aria-busy={loading}
        >

          {obras.map((obra) => (

            <Obra
              key={obra.id}
              obra={obra}
            />

          ))}

        </div>

        {!error &&
          obras.length > 0 && (

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
                  obras.length < TAMANIO_PAGINA
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
