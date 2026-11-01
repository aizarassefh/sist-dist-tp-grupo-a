import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useQuery } from '@apollo/client/react'
import { compararObras, mensajeDeErrorGateway } from '../api/gateway'
import { OBRAS_LISTADO_QUERY } from '../graphql/queries'
import ImagenObra from '../components/ImagenObra'
import Trazas from '../components/Trazas'

const MINIMO_OBRAS = 2
const MAXIMO_OBRAS = 6
// El listado del catálogo es paginado; se pide una página grande para poder
// elegir entre todas las obras sin armar un buscador aparte.
const OBRAS_A_LISTAR = 100

const FILTRO_VACIO = {
  palabraClave: null,
  epoca: null,
  tecnica: null,
  ubicacion: null,
  enExhibicion: null
}

function Comparar() {
  const [searchParams] = useSearchParams()

  // Desde la ficha de una obra se llega con ?obra=ID. Se lee una sola vez:
  // después la selección vive en el estado y no en la URL.
  const [seleccion, setSeleccion] = useState(() =>
    [...new Set(searchParams.getAll('obra'))].slice(0, MAXIMO_OBRAS)
  )
  const [busqueda, setBusqueda] = useState('')
  const [comparando, setComparando] = useState(false)
  const [errorComparacion, setErrorComparacion] = useState(null)
  const [comparacion, setComparacion] = useState(null)

  const { loading, error, data } = useQuery(OBRAS_LISTADO_QUERY, {
    variables: { filtro: FILTRO_VACIO, pagina: 0, tamanio: OBRAS_A_LISTAR }
  })

  const obras = data?.obras ?? []
  const textoBusqueda = busqueda.trim().toLowerCase()
  const obrasVisibles = obras.filter((obra) =>
    `${obra.titulo} ${obra.artista.nombre}`.toLowerCase().includes(textoBusqueda)
  )
  const completa = seleccion.length >= MAXIMO_OBRAS

  function alternar(id) {
    setSeleccion((actual) =>
      actual.includes(id) ? actual.filter((elegida) => elegida !== id) : [...actual, id]
    )
  }

  // Una obra preseleccionada puede no estar en el listado cargado: se
  // muestra igual con su número para que se pueda quitar.
  function nombreDe(id) {
    const obra = obras.find((candidata) => candidata.id === id)
    return obra ? obra.titulo : `Obra ${id}`
  }

  async function comparar() {
    setComparando(true)
    setErrorComparacion(null)

    try {
      setComparacion(await compararObras(seleccion.map(Number)))
    } catch (falla) {
      setComparacion(null)
      setErrorComparacion(mensajeDeErrorGateway(falla))
    } finally {
      setComparando(false)
    }
  }

  return (
    <>
      <header className="encabezado">
        <h1>Comparar obras</h1>
        <p>
          Elegí de {MINIMO_OBRAS} a {MAXIMO_OBRAS} obras y mirá en qué se
          parecen y en qué no.
        </p>
      </header>

      <section className="comparar-seleccion">
        <p className="rotulo">
          Elegidas: {seleccion.length} de {MAXIMO_OBRAS}
        </p>

        {seleccion.length > 0 && (
          <ul className="comparar-elegidas">
            {seleccion.map((id) => (
              <li key={id}>
                {nombreDe(id)}
                <button
                  type="button"
                  className="comparar-quitar"
                  aria-label={`Quitar ${nombreDe(id)}`}
                  onClick={() => alternar(id)}
                >
                  ×
                </button>
              </li>
            ))}
          </ul>
        )}

        <div className="campo comparar-busqueda">
          <label htmlFor="busqueda-obras">Buscar en la colección</label>
          <input
            id="busqueda-obras"
            type="search"
            placeholder="Título o artista"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </div>

        {loading && <p className="ficha-mensaje">Cargando obras...</p>}

        {error && <p className="error">Error: {error.message}</p>}

        {!loading && !error && obrasVisibles.length === 0 && (
          <p className="ficha-vacio">No hay obras que coincidan con la búsqueda.</p>
        )}

        <ul className="comparar-lista">
          {obrasVisibles.map((obra) => {
            const elegida = seleccion.includes(obra.id)

            return (
              <li key={obra.id}>
                <label className="selector-opcion comparar-opcion">
                  <input
                    type="checkbox"
                    checked={elegida}
                    disabled={!elegida && completa}
                    onChange={() => alternar(obra.id)}
                  />
                  <span>
                    <em>{obra.titulo}</em>, {obra.artista.nombre}
                  </span>
                </label>
              </li>
            )
          })}
        </ul>

        <div className="formulario-gateway-acciones">
          <button
            type="button"
            onClick={comparar}
            disabled={comparando || seleccion.length < MINIMO_OBRAS}
          >
            {comparando ? 'Comparando...' : 'Comparar'}
          </button>

          {seleccion.length < MINIMO_OBRAS && (
            <span className="motivo-deshabilitado">
              Elegí al menos {MINIMO_OBRAS} obras.
            </span>
          )}
        </div>
      </section>

      {errorComparacion && <p className="error">{errorComparacion}</p>}

      {comparacion && (
        <section className="resultado" aria-live="polite">
          <div className="tabla-scroll">
            <table className="tabla-comparacion">
              <thead>
                <tr>
                  <th scope="col"><span className="sr-only">Atributo</span></th>
                  {comparacion.obras.map((obra) => (
                    <th key={obra.id} scope="col">
                      <Link to={`/obra/${obra.id}`}>
                        <ImagenObra
                          className="comparacion-imagen"
                          src={obra.imagenUrl}
                          alt={`${obra.titulo}, de ${obra.artista.nombre}`}
                        />
                        <span className="comparacion-titulo">{obra.titulo}</span>
                      </Link>
                    </th>
                  ))}
                </tr>
              </thead>

              <tbody>
                {comparacion.atributos.map((atributo) => (
                  <tr key={atributo.nombre} className={atributo.coinciden ? 'coinciden' : undefined}>
                    <th scope="row">
                      {atributo.nombre}
                      {atributo.coinciden && <span className="coinciden-marca">Coinciden</span>}
                    </th>
                    {atributo.valores.map((valor, indice) => (
                      <td key={indice}>{valor ?? '—'}</td>
                    ))}
                  </tr>
                ))}

                <tr>
                  <th scope="row">Descripción</th>
                  {comparacion.obras.map((obra) => (
                    <td key={obra.id} className="comparacion-descripcion">
                      {obra.descripcion}
                    </td>
                  ))}
                </tr>
              </tbody>
            </table>
          </div>

          {comparacion.observaciones?.length > 0 && (
            <>
              <h2 className="rotulo">Observaciones</h2>
              <ul className="notas">
                {comparacion.observaciones.map((observacion, indice) => (
                  <li key={indice}>{observacion}</li>
                ))}
              </ul>
            </>
          )}

          <Trazas trazas={comparacion.trazas} />
        </section>
      )}
    </>
  )
}

export default Comparar
