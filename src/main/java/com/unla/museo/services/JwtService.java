package com.unla.museo.services;

import io.jsonwebtoken.Claims;

/**
 * Servicio unificado para gestión de tokens JWT.
 */
public interface JwtService {

    /**
     * Genera un Access Token JWT firmado con el rol único del usuario.
     */
    String generateAccessToken(String username);

    /**
     * Extrae todos los claims del JWT.
     */
    Claims extractAllClaims(String token);

    /**
     * Extrae el username del JWT.
     */
    String extractUsername(String token);

    /**
     * Valida que el JWT sea válido (firma y no expirado).
     */
    boolean isTokenValid(String token);
}
