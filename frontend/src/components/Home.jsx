function Home({ onExplorar }) {
  return (
    <section className="inicio">

      <div className="inicio-contenido">

        <p className="inicio-etiqueta">
          MUSEO VIRTUAL
        </p>

        <h2>
          Explorá nuestra colección
        </h2>

        <p className="inicio-descripcion">
          Descubrí obras de distintos períodos,
          artistas y movimientos de la historia del arte.
        </p>

        <button
          className="boton-explorar"
          type="button"
          onClick={onExplorar}
        >
          Explorar colección
        </button>

      </div>

      <div className="inicio-informacion">

        <div className="inicio-tarjeta">

          <h3>Obras de arte</h3>

          <p>
            Consultá nuestra colección y conocé
            información sobre cada obra.
          </p>

        </div>

        <div className="inicio-tarjeta">

          <h3>Artistas</h3>

          <p>
            Conocé a los artistas y sus principales
            obras.
          </p>

        </div>

        <div className="inicio-tarjeta">

          <h3>Exploración</h3>

          <p>
            Utilizá filtros para encontrar obras
            según tus intereses.
          </p>

        </div>

      </div>

    </section>
  )
}

export default Home