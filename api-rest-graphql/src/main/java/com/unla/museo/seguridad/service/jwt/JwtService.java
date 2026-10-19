package com.unla.museo.seguridad.service.jwt;

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
     * Extrae todos los claims del JWT: firma inválida o token vencido lanzan
     * JwtException (subclase de RuntimeException).
     */
    Claims extractAllClaims(String token);

    /**
     * Cuánto dura un token recién emitido, para informarlo en la respuesta del login.
     */
    long duracionEnSegundos();
}
