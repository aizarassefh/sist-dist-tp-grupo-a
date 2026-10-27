import { Link } from 'react-router-dom'
import ImagenObra from './ImagenObra'
import { ROLES, esGestor, tieneRol } from '../roles'

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

const SECCION_COMPARAR = {
  ruta: '/comparar',
  titulo: 'Comparar obras',
  texto: 'Poné de dos a seis obras lado a lado y mirá qué tienen en común.'
}

const SECCION_EXPLORAR = {
  ruta: '/explorar',
  titulo: 'Explorar por movimiento',
  texto: 'Las obras, los artistas y los eventos de cada movimiento artístico.'
}

// Estas dos dependen del perfil del visitante (preferencias e interacciones).
const SECCIONES_VISITANTE = [
  {
    ruta: '/recorridos',
    titulo: 'Recorridos',
    texto: 'Un recorrido por las salas armado a la medida de tu tiempo y tus gustos.'
  },
  {
    ruta: '/recomendaciones',
    titulo: 'Recomendaciones',
    texto: 'Obras que te pueden gustar, según tus preferencias y lo que viste.'
  }
]

const SECCION_REPORTE = {
  ruta: '/reporte',
  titulo: 'Reporte de asistencia',
  texto: 'Inscripciones por mes y por tipo de evento, y exportación a Excel.'
}

function Home({ usuario }) {
  const secciones = [
    ...SECCIONES,
    ...(tieneRol(usuario, ROLES.VISITANTE) ? SECCIONES_VISITANTE : []),
    SECCION_COMPARAR,
    SECCION_EXPLORAR,
    ...(esGestor(usuario) ? [SECCION_REPORTE] : [])
  ]

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
