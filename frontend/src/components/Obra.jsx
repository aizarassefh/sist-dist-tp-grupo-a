import { Link } from 'react-router-dom'
import ImagenObra from './ImagenObra'

// Una pieza colgada en la pared: la obra manda y debajo va la cartela, como
// en sala. Toda la pieza lleva al detalle.
function Obra({ obra }) {
  return (
    <article className="pieza">
      <Link className="pieza-enlace" to={`/obra/${obra.id}`}>

        <ImagenObra
          className="pieza-marco"
          diferida
          src={obra.imagenUrl}
          alt={`${obra.titulo}, de ${obra.artista.nombre}`}
        />

        <div className="cartela">
          <h2 className="cartela-titulo">{obra.titulo}</h2>

          <p className="cartela-artista">
            {obra.artista.nombre}, {obra.anioCreacion}
          </p>

          <p className="cartela-datos">
            {obra.epoca} · {obra.ubicacion}
          </p>

          <span className={obra.enExhibicion ? 'estado exhibicion' : 'estado deposito'}>
            {obra.enExhibicion ? 'En exhibición' : 'En depósito'}
          </span>
        </div>

      </Link>
    </article>
  )
}

export default Obra
