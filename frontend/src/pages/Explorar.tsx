import { Link } from 'react-router-dom'
import { mensajeDeErrorGateway, obtenerCategorias } from '../api/gateway'
import { useCarga } from '../api/useCarga'

function Explorar() {
  const categorias = useCarga('categorias', obtenerCategorias)
  const movimientos = categorias.datos?.movimientos ?? []

  return (
    <>
      <header className="encabezado">
        <h1>Explorar</h1>
        <p>
          Elegí un movimiento artístico y mirá sus obras, sus artistas y los
          eventos del museo que se vinculan con él.
        </p>
      </header>

      {categorias.cargando && <p className="ficha-mensaje">Cargando movimientos...</p>}

      {categorias.error && <p className="error">{mensajeDeErrorGateway(categorias.error)}</p>}

      {categorias.datos && movimientos.length === 0 && (
        <p className="ficha-vacio">Todavía no hay movimientos cargados.</p>
      )}

      {movimientos.length > 0 && (
        <nav className="movimientos" aria-label="Movimientos artísticos">
          {movimientos.map((movimiento) => (
            <Link
              key={movimiento.nombre}
              className="inicio-indice-fila"
              to={`/explorar/${encodeURIComponent(movimiento.nombre)}`}
            >
              <span className="inicio-indice-titulo">{movimiento.nombre}</span>
              <span className="inicio-indice-texto">
                {movimiento.cantidadObras} {movimiento.cantidadObras === 1 ? 'obra' : 'obras'}
              </span>
            </Link>
          ))}
        </nav>
      )}
    </>
  )
}

export default Explorar
