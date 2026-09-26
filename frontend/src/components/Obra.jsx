import { Link } from 'react-router-dom'

function Obra({ obra }) {
  return (
    <article className="obra">

      {obra.imagenUrl ? (

        <img
          className="obra-imagen"
          src={obra.imagenUrl}
          alt={`Obra ${obra.titulo}`}
          onError={(e) => {
            e.currentTarget.style.display = 'none'
          }}
        />

      ) : (

        <div className="obra-sin-imagen">
          Sin imagen disponible
        </div>

      )}

      <div className="obra-informacion">

        <h2>{obra.titulo}</h2>

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

        <p className="obra-descripcion">
          {obra.descripcion}
        </p>

        <div className="comentarios">

          <h3>Comentarios</h3>

          {obra.comentarios.length === 0 ? (

            <p className="sin-comentarios">
              No hay comentarios todavía.
            </p>

          ) : (

            obra.comentarios.map((comentario, index) => (

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

            ))

          )}

        </div>

        <Link
          className="boton-detalle"
          to={`/obra/${obra.id}`}
        >
          Ver detalles
        </Link>

      </div>

    </article>
  )
}

export default Obra