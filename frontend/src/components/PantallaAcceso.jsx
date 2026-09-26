import ImagenObra from './ImagenObra'

// Login y registro comparten la misma pantalla partida: una obra de un lado
// y el formulario del otro. Lo primero que se ve del museo es una obra.
function PantallaAcceso({ children }) {
  return (
    <div className="acceso">

      <figure className="acceso-obra">
        <ImagenObra
          className="acceso-imagen"
          src="/obras/gioconda.jpg"
          alt="La Gioconda, de Leonardo da Vinci"
        />

        <figcaption className="acceso-cartela">
          <span className="acceso-marca">Museo Virtual</span>
          <span>
            <em>La Gioconda</em>, Leonardo da Vinci, 1503
          </span>
        </figcaption>
      </figure>

      <div className="acceso-formulario">
        {children}
      </div>

    </div>
  )
}

export default PantallaAcceso
