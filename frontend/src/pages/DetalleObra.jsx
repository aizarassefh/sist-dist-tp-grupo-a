import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@apollo/client/react'
import { OBRA_DETALLE_QUERY } from '../graphql/queries'

function DetalleObra() {
  const { id } = useParams()

  const { loading, error, data } = useQuery(
    OBRA_DETALLE_QUERY,
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

export default DetalleObra
