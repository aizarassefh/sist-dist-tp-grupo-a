import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@apollo/client/react'
import { OBRA_DETALLE_QUERY } from '../graphql/queries'
import ImagenObra from '../components/ImagenObra'

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
    return <p className="ficha-mensaje">Cargando obra...</p>
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
      <section className="sin-permiso">

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
    <article className="ficha-obra">

      <Link
        className="enlace-volver"
        to="/coleccion"
      >
        ← Colección
      </Link>

      <div className="ficha-contenido">

        <div className="ficha-imagen">
          <ImagenObra
            className="ficha-marco"
            src={obra.imagenUrl}
            alt={`${obra.titulo}, de ${obra.artista.nombre}`}
          />
        </div>

        <div className="ficha-texto">

          <p className="rotulo">{obra.epoca}</p>

          <h1 className="ficha-titulo">
            {obra.titulo}
          </h1>

          <p className="ficha-artista">
            {obra.artista.nombre}
          </p>

          <dl className="ficha-datos">
            <div>
              <dt>Año</dt>
              <dd>{obra.anioCreacion}</dd>
            </div>

            <div>
              <dt>Técnica</dt>
              <dd>{obra.tecnica}</dd>
            </div>

            <div>
              <dt>Dimensiones</dt>
              <dd>{obra.dimensiones}</dd>
            </div>

            <div>
              <dt>Ubicación</dt>
              <dd>{obra.ubicacion}</dd>
            </div>

            <div>
              <dt>Estado</dt>
              <dd>
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
              </dd>
            </div>
          </dl>

          <p className="ficha-descripcion">
            {obra.descripcion}
          </p>

          <section className="ficha-seccion">
            <h2 className="rotulo">Sobre el artista</h2>

            <p>
              {obra.artista.biografia}
            </p>
          </section>

          <section className="ficha-seccion">
            <h2 className="rotulo">
              Comentarios ({obra.comentarios.length})
            </h2>

            {obra.comentarios.length === 0 ? (

              <p className="ficha-vacio">
                No hay comentarios todavía.
              </p>

            ) : (

              <ul className="comentarios-lista">
                {obra.comentarios.map((comentario, index) => (
                  <li key={index}>
                    <p className="comentario-texto">
                      {comentario.texto}
                    </p>

                    <p className="comentario-autor">
                      {comentario.usuario} · {comentario.fecha}
                    </p>
                  </li>
                ))}
              </ul>

            )}
          </section>

        </div>

      </div>

    </article>
  )
}

export default DetalleObra
