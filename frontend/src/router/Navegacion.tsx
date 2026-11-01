import { Link, NavLink } from 'react-router-dom'
import { ROLES, esGestor, tieneRol } from './roles'

function Navegacion({ user, onLogout }) {
  return (
    <nav className="navegacion">
      <Link to="/" className="navegacion-logo">Museo Virtual</Link>

      <div className="navegacion-links">
        <NavLink to="/" end className="navegacion-link">Inicio</NavLink>
        <NavLink to="/coleccion" className="navegacion-link">Colección</NavLink>
        <NavLink to="/eventos" className="navegacion-link">Eventos</NavLink>
        <NavLink to="/favoritos" className="navegacion-link">Mis filtros</NavLink>

        {tieneRol(user, ROLES.VISITANTE) && (
          <NavLink to="/recorridos" className="navegacion-link">Recorridos</NavLink>
        )}

        <NavLink to="/comparar" className="navegacion-link">Comparar</NavLink>

        {tieneRol(user, ROLES.VISITANTE) && (
          <NavLink to="/recomendaciones" className="navegacion-link">Recomendaciones</NavLink>
        )}

        <NavLink to="/explorar" className="navegacion-link">Explorar</NavLink>

        {esGestor(user) && (
          <NavLink to="/reporte" className="navegacion-link">Reporte</NavLink>
        )}

        <span className="usuario-info">
          {user?.nombre} {user?.apellido} · {user?.rol}
        </span>

        <button type="button" onClick={onLogout} className="navegacion-salir">
          Cerrar sesión
        </button>
      </div>
    </nav>
  )
}

export default Navegacion
