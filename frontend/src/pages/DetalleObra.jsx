import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@apollo/client/react'
import { OBRA_DETALLE_QUERY } from '../graphql/queries'
import { mensajeDeErrorGateway, registrarInteraccion } from '../api/gateway'
import { ROLES, tieneRol } from '../roles'
import ImagenObra from '../components/ImagenObra'

function DetalleObra({ usuario }) {
  const { id } = useParams()
  const esVisitante = tieneRol(usuario, ROLES.VISITANTE)

  // 'enviando' | 'registrado' | mensaje de error | null
  const [interes, setInteres] = useState(null)
  const vistaRegistradaDe = useRef(null)

  const { loading, error, data } = useQuery(
    OBRA_DETALLE_QUERY,
    {
      variables: { id }
    }
  )

  const obraId = data?.obra?.id

  // Solo cuenta como vista si la obra existe. La ref evita que StrictMode
  // (que monta dos veces en desarrollo) registre la misma vista dos veces.
  useEffect(() => {
    if (!esVisitante || !obraId || vistaRegistradaDe.current === obraId) {
      return
    }
    vistaRegistradaDe.current = obraId

    // La vista alimenta las recomendaciones pero no es crítica: si falla
    // (gateway caído, por ejemplo) no se le muestra nada al visitante.
    registrarInteraccion(Number(obraId), 'VISTA').catch(() => {})
  }, [esVisitante, obraId])

  async function marcarInteres() {
    setInteres('enviando')

    try {
      await registrarInteraccion(Number(obraId), 'INTERES')
      setInteres('registrado')
    } catch (error) {
      setInteres(mensajeDeErrorGateway(error))
    }
  }

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

  const obra = data?.obra

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

          <div className="ficha-acciones">
            {esVisitante && (
              <button
                type="button"
                onClick={marcarInteres}
                disabled={interes === 'enviando' || interes === 'registrado'}
              >
                {interes === 'registrado' ? 'Te interesa' : 'Me interesa'}
              </button>
            )}

            <Link
              className="boton-detalle"
              to={`/comparar?obra=${obra.id}`}
            >
              Comparar con otras obras
            </Link>
          </div>

          {interes === 'registrado' && (
            <p className="aviso-ok" role="status">
              Anotamos tu interés. Lo vamos a tener en cuenta en tus{' '}
              <Link to="/recomendaciones">recomendaciones</Link>.
            </p>
          )}

          {interes && interes !== 'enviando' && interes !== 'registrado' && (
            <p className="error">{interes}</p>
          )}

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
