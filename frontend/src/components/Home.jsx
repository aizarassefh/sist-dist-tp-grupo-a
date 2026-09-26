import { Link } from 'react-router-dom'
import ImagenObra from './ImagenObra'
import { esGestor } from '../roles'

// Un índice en filas y no tres tarjetas iguales: cada sección se nombra con
// lo que se hace ahí.
const SECCIONES = [
  {
    ruta: '/coleccion',
    titulo: 'La colección',
    texto: 'Obras del Renacimiento al siglo XX. Buscá por artista, época, técnica o sala.'
  },
  {
    ruta: '/eventos',
    titulo: 'Agenda',
    texto: 'Visitas guiadas, talleres y charlas. Anotate y cancelá cuando quieras.'
  },
  {
    ruta: '/favoritos',
    titulo: 'Mis filtros',
    texto: 'Las búsquedas de eventos que guardaste, para aplicarlas de nuevo.'
  }
]

const SECCION_REPORTE = {
  ruta: '/reporte',
  titulo: 'Reporte de asistencia',
  texto: 'Inscripciones por mes y por tipo de evento, y exportación a Excel.'
}

function Home({ usuario }) {
  const secciones = esGestor(usuario)
    ? [...SECCIONES, SECCION_REPORTE]
    : SECCIONES

  return (
    <div className="inicio">

      <section className="inicio-portada">

        <div className="inicio-texto">
          <p className="rotulo">Museo Virtual</p>

          <h1>
            Más de cuatro siglos de pintura, a una búsqueda de distancia.
          </h1>

          <p className="inicio-bajada">
            Recorré la colección, conocé a sus artistas y sumate a las
            visitas guiadas, talleres y charlas del museo.
          </p>

          <div className="inicio-acciones">
            <Link className="boton-generar" to="/coleccion">
              Recorrer la colección
            </Link>

            <Link className="boton-detalle" to="/eventos">
              Ver la agenda
            </Link>
          </div>
        </div>

        <figure className="inicio-obra">
          <ImagenObra
            className="inicio-imagen"
            src="/obras/el-grito.jpg"
            alt="El grito, de Edvard Munch"
          />

          <figcaption className="inicio-cartela">
            <em>El grito</em>, Edvard Munch, 1893
          </figcaption>
        </figure>

      </section>

      <nav className="inicio-indice" aria-label="Secciones del museo">
        {secciones.map((seccion) => (
          <Link
            key={seccion.ruta}
            className="inicio-indice-fila"
            to={seccion.ruta}
          >
            <span className="inicio-indice-titulo">{seccion.titulo}</span>
            <span className="inicio-indice-texto">{seccion.texto}</span>
          </Link>
        ))}
      </nav>

    </div>
  )
}

export default Home
