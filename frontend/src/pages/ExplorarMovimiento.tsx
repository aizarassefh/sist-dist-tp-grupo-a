import { Link, useParams } from 'react-router-dom'
import { explorarMovimiento, mensajeDeErrorGateway } from '../api/gateway'
import { useCarga } from '../api/useCarga'
import { formatearFechaHora } from '../fechas'
import { nombreTipo } from '../tiposEvento'
import ObraCompacta from '../components/ObraCompacta'
import Trazas from '../components/Trazas'

function textoAnios(artista) {
  const desde = artista.anioNacimiento
  const hasta = artista.anioFallecimiento

  if (desde && hasta) {
    return `${desde}–${hasta}`
  }
  // Un artista vivo no tiene año de fallecimiento: no es un dato faltante.
  return desde ? `n. ${desde}` : null
}

function textoCupo(evento) {
  const libres = evento.cupoMaximo - evento.inscriptos
  return libres > 0
    ? `${libres} de ${evento.cupoMaximo} lugares libres`
    : `Cupo completo (${evento.cupoMaximo})`
}

function ExplorarMovimiento() {
  // useParams ya devuelve el segmento decodificado.
  const { movimiento } = useParams()
  const exploracion = useCarga(`exploracion-${movimiento}`, () => explorarMovimiento(movimiento))
  const datos = exploracion.datos

  return (
    <>
      <Link className="enlace-volver" to="/explorar">
        ← Movimientos
      </Link>

      <header className="encabezado">
        <p className="rotulo">Movimiento</p>
        <h1>{movimiento}</h1>
      </header>

      {exploracion.cargando && <p className="ficha-mensaje">Explorando {movimiento}...</p>}

      {exploracion.error && <p className="error">{mensajeDeErrorGateway(exploracion.error)}</p>}

      {datos && (
        <>
          <section className="resultado-seccion">
            <h2 className="rotulo">Obras ({datos.obras.length})</h2>

            {datos.obras.length === 0 ? (
              <p className="ficha-vacio">No hay obras de este movimiento.</p>
            ) : (
              <div className="galeria">
                {datos.obras.map((obra) => (
                  <ObraCompacta key={obra.id} obra={obra} />
                ))}
              </div>
            )}
          </section>

          <section className="resultado-seccion">
            <h2 className="rotulo">Artistas ({datos.artistas.length})</h2>

            {datos.artistas.length === 0 ? (
              <p className="ficha-vacio">No hay artistas de este movimiento.</p>
            ) : (
              <ul className="artistas">
                {datos.artistas.map((artista) => (
                  <li key={artista.id} className="artista">
                    <h3>{artista.nombre}</h3>
                    <p className="artista-datos">
                      {[artista.nacionalidad, textoAnios(artista)].filter(Boolean).join(' · ')}
                      {' · '}
                      {artista.cantidadObras} {artista.cantidadObras === 1 ? 'obra' : 'obras'}
                    </p>
                    {artista.biografia && (
                      <p className="artista-biografia" title={artista.biografia}>
                        {artista.biografia}
                      </p>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="resultado-seccion">
            <h2 className="rotulo">Eventos vinculados ({datos.eventos.length})</h2>

            {datos.eventos.length === 0 ? (
              <p className="ficha-vacio">No hay eventos próximos vinculados a este movimiento.</p>
            ) : (
              <ul className="eventos-vinculados">
                {datos.eventos.map((evento) => (
                  <li key={evento.id}>
                    <Link className="inicio-indice-fila" to={`/eventos/${evento.id}`}>
                      <span className="inicio-indice-titulo">{evento.titulo}</span>
                      <span className="inicio-indice-texto">
                        {nombreTipo(evento.tipo)} · {formatearFechaHora(evento.fechaHora)} ·{' '}
                        {textoCupo(evento)}
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </section>

          {datos.otrosMovimientos.length > 0 && (
            <section className="resultado-seccion">
              <h2 className="rotulo">Seguir explorando</h2>

              <ul className="otros-movimientos">
                {datos.otrosMovimientos.map((otro) => (
                  <li key={otro.nombre}>
                    <Link to={`/explorar/${encodeURIComponent(otro.nombre)}`}>
                      {otro.nombre}
                      <span className="selector-cantidad">{otro.cantidadObras}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            </section>
          )}

          <Trazas trazas={datos.trazas} />
        </>
      )}
    </>
  )
}

export default ExplorarMovimiento
