export const ROLES = {
  VISITANTE: 'VISITANTE',
  CURADOR: 'CURADOR',
  ADMINISTRADOR: 'ADMINISTRADOR',
}

// /api/auth/me devuelve el nombre del rol, que según cómo se cargó la base
// puede venir como "Curador" o "CURADOR"; por eso se compara en mayúsculas.
export function tieneRol(usuario, ...roles) {
  const rol = usuario?.role?.toUpperCase()
  return rol !== undefined && roles.includes(rol)
}

export function esGestor(usuario) {
  return tieneRol(usuario, ROLES.CURADOR, ROLES.ADMINISTRADOR)
}
