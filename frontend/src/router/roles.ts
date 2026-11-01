export const ROLES = {
  VISITANTE: 'VISITANTE',
  CURADOR: 'CURADOR',
  ADMINISTRADOR: 'ADMINISTRADOR',
}

// /api/auth/me devuelve el nombre del rol, que según cómo se cargó la base
// puede venir como "Curador" o "CURADOR"; por eso se compara en mayúsculas.
export function tieneRol(usuario: Usuario | null, ...roles: string[]): boolean {
  const rol = usuario?.rol?.toUpperCase()
  return rol !== undefined && roles.includes(rol)
}

export function esGestor(usuario: Usuario | null): boolean {
  return tieneRol(usuario, ROLES.CURADOR, ROLES.ADMINISTRADOR)
}
import type { Usuario } from '../types/modelos'
