import { Link } from 'react-router-dom'
import { tieneRol } from '../roles'

// El backend es el que realmente protege los datos; esto evita que un
// usuario sin permiso que escribe la URL a mano vea una pantalla rota.
function RequiereRol({ usuario, roles, children }) {
  if (tieneRol(usuario, ...roles)) {
    return children
  }

  return (
    <section className="sin-permiso">
      <h2>No tenés permisos para ver esta página</h2>
      <p>Esta sección está disponible para: {roles.join(', ').toLowerCase()}.</p>
      <Link className="boton-volver" to="/">
        ← Volver al inicio
      </Link>
    </section>
  )
}

export default RequiereRol
