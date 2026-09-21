package com.unla.museo.services;

import io.jsonwebtoken.Claims;

import java.util.List;

/**
 * Servicio unificado para gestión de tokens JWT y Refresh Tokens
 */
public interface JwtService {

    /**
     * Genera un Access Token JWT firmado
     * @param username nombre del usuario
     * @return Access Token JWT
     */
    String generateAccessToken(String username);

    /**
     * Genera un Refresh Token y lo almacena en BD
     * @param email email del usuario
     * @return entidad del Refresh Token
     */

    /**
     * Extrae todos los claims del JWT
     * @param token token JWT
     * @return claims del token
     */
    Claims extractAllClaims(String token);

    /**
     * Extrae el username del JWT
     * @param token token JWT
     * @return email del usuario
     */
    String extractUsername(String token);

    /**
     * Extrae la lista de permisos del JWT
     * @param token token JWT
     * @return lista de permisos
     */
    List<String> extractPermissions(String token);

    /**
     * Valida que el JWT sea válido (firma y no expirado)
     * @param token token JWT
     * @return true si es válido, false en caso contrario
     */
    boolean isTokenValid(String token);

}
