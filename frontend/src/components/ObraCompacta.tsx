import { Link } from 'react-router-dom'
import ImagenObra from './ImagenObra'

// Misma pieza que Obra.jsx pero para la forma de obra del gateway
// (docs/contrato-api-gateway.md), que no trae "epoca" y sí "movimiento", y
// donde el año puede venir en null.
function ObraCompacta({ obra }) {
  const datos = [obra.movimiento, obra.ubicacion].filter(Boolean).join(' · ')

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
          <h3 className="cartela-titulo">{obra.titulo}</h3>

          <p className="cartela-artista">
            {[obra.artista.nombre, obra.anioCreacion].filter(Boolean).join(', ')}
          </p>

          {datos && <p className="cartela-datos">{datos}</p>}
        </div>

      </Link>
    </article>
  )
}

export default ObraCompacta
