import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  guardarPreferencias,
  mensajeDeErrorGateway,
  obtenerCategorias,
  obtenerPreferencias,
  obtenerRecomendaciones
} from '../api/gateway'
import { useCarga } from '../api/useCarga'
import ImagenObra from '../components/ImagenObra'
import SelectorMultiple from '../components/SelectorMultiple'
import Trazas from '../components/Trazas'

const CANTIDAD_RECOMENDACIONES = 6

const PREFERENCIAS_VACIAS = { movimientos: [], tecnicas: [], tematicas: [] }

const DESCRIPCION_CRITERIO = {
  PERSONALIZADO: 'Personalizado: armado con tus preferencias y tu actividad en el museo.',
  EXPLORATORIO: 'Exploratorio: todavía no hay datos tuyos suficientes para personalizar.'
}

function Recomendaciones() {
  // Contador que se incrementa al guardar preferencias para volver a pedir
  // las recomendaciones, que dependen de ellas.
  const [version, setVersion] = useState(0)

  const categorias = useCarga('categorias', obtenerCategorias)
  const preferencias = useCarga('preferencias', obtenerPreferencias)
  const recomendaciones = useCarga(`recomendaciones-${version}`, () =>
    obtenerRecomendaciones(CANTIDAD_RECOMENDACIONES)
  )

  // "edicion" es null hasta que el usuario toca algo: mientras tanto se
  // muestran las preferencias guardadas, sin copiarlas a mano al estado.
  const [edicion, setEdicion] = useState(null)
  const [guardadas, setGuardadas] = useState(null)
  const [guardando, setGuardando] = useState(false)
  const [errorGuardado, setErrorGuardado] = useState(null)
  const [avisoGuardado, setAvisoGuardado] = useState(false)

  const base = guardadas ?? preferencias.datos ?? PREFERENCIAS_VACIAS
  const actuales = edicion ?? base

  function cambiar(campo, valor) {
    setAvisoGuardado(false)
    setEdicion({ ...actuales, [campo]: valor })
  }

  async function guardar(evento) {
    evento.preventDefault()
    setGuardando(true)
    setErrorGuardado(null)
    setAvisoGuardado(false)

    try {
      const respuesta = await guardarPreferencias({
        movimientos: actuales.movimientos,
        tecnicas: actuales.tecnicas,
        tematicas: actuales.tematicas
      })
      setGuardadas(respuesta)
      setEdicion(null)
      setAvisoGuardado(true)
      setVersion((actual) => actual + 1)
    } catch (error) {
      setErrorGuardado(mensajeDeErrorGateway(error))
    } finally {
      setGuardando(false)
    }
  }

  const opciones = categorias.datos
  const resultado = recomendaciones.datos

  return (
    <>
      <header className="encabezado">
        <h1>Recomendaciones</h1>
        <p>
          Obras que te pueden interesar, según lo que marcaste como
          preferido, lo que viste y los eventos a los que te anotaste.
        </p>
      </header>

      <section className="resultado-seccion">
        <h2 className="rotulo">Mis preferencias</h2>

        {preferencias.error && (
          <p className="error">
            No se pudieron cargar tus preferencias: {mensajeDeErrorGateway(preferencias.error)}
          </p>
        )}

        {categorias.error && (
          <p className="error">
            No se pudieron cargar las categorías: {mensajeDeErrorGateway(categorias.error)}
          </p>
        )}

        {(preferencias.cargando || categorias.cargando) && (
          <p className="ficha-mensaje">Cargando preferencias...</p>
        )}

        {opciones && preferencias.datos && (
          <form className="formulario-gateway" onSubmit={guardar}>
            <SelectorMultiple
              titulo="Movimientos"
              opciones={opciones.movimientos}
              seleccion={actuales.movimientos}
              onCambiar={(valor) => cambiar('movimientos', valor)}
            />
            <SelectorMultiple
              titulo="Técnicas"
              opciones={opciones.tecnicas}
              seleccion={actuales.tecnicas}
              onCambiar={(valor) => cambiar('tecnicas', valor)}
            />
            <SelectorMultiple
              titulo="Temáticas"
              opciones={opciones.tematicas}
              seleccion={actuales.tematicas}
              onCambiar={(valor) => cambiar('tematicas', valor)}
            />

            <div className="formulario-gateway-acciones">
              <button type="submit" disabled={guardando}>
                {guardando ? 'Guardando...' : 'Guardar preferencias'}
              </button>

              {avisoGuardado && (
                <span className="aviso-ok" role="status">Preferencias guardadas.</span>
              )}
            </div>

            {errorGuardado && <p className="error">{errorGuardado}</p>}
          </form>
        )}
      </section>

      <section className="resultado-seccion">
        <h2 className="rotulo">Para vos</h2>

        {recomendaciones.cargando && (
          <p className="ficha-mensaje">Buscando recomendaciones...</p>
        )}

        {recomendaciones.error && (
          <p className="error">{mensajeDeErrorGateway(recomendaciones.error)}</p>
        )}

        {resultado && (
          <>
            <p className="criterio">
              <span className={`criterio-sello criterio-${resultado.criterio.toLowerCase()}`}>
                {resultado.criterio}
              </span>
              {DESCRIPCION_CRITERIO[resultado.criterio]}
            </p>

            {resultado.recomendaciones.length === 0 ? (
              <p className="ficha-vacio">
                Por ahora no hay obras para recomendarte.
              </p>
            ) : (
              <ol className="recorrido-paradas">
                {resultado.recomendaciones.map((recomendacion) => (
                  <li key={recomendacion.obra.id} className="parada">
                    <span className="parada-numero parada-puntaje">
                      {recomendacion.puntaje.toFixed(1)}
                      <small>puntos</small>
                    </span>

                    <ImagenObra
                      className="parada-imagen"
                      src={recomendacion.obra.imagenUrl}
                      alt={`${recomendacion.obra.titulo}, de ${recomendacion.obra.artista.nombre}`}
                    />

                    <div className="parada-texto">
                      <h3 className="parada-titulo">
                        <Link to={`/obra/${recomendacion.obra.id}`}>{recomendacion.obra.titulo}</Link>
                      </h3>
                      <p className="parada-artista">{recomendacion.obra.artista.nombre}</p>
                      <ul className="motivos">
                        {recomendacion.motivos.map((motivo, indice) => (
                          <li key={indice}>{motivo}</li>
                        ))}
                      </ul>
                    </div>
                  </li>
                ))}
              </ol>
            )}

            <Trazas trazas={resultado.trazas} />
          </>
        )}
      </section>
    </>
  )
}

export default Recomendaciones
