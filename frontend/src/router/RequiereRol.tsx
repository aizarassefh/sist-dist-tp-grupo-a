import { Link } from 'react-router-dom'
import type { ReactNode } from 'react'
import { tieneRol } from './roles'
import type { Usuario } from '../types/modelos'

interface RequiereRolProps {
  usuario: Usuario | null
  roles: string[]
  children: ReactNode
}

// El backend es el que realmente protege los datos; esto evita que un
// usuario sin permiso que escribe la URL a mano vea una pantalla rota.
function RequiereRol({ usuario, roles, children }: RequiereRolProps): ReactNode {
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
