package com.unla.museo.helper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
public class JwtHelper {

    /**
     * Extrae el email del token.
     */
    public static String extractEmailFromToken(String idToken, String secret) {
        try {
            Claims claims = decodeToken(idToken, secret);
            String email = claims.get("email", String.class);

            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Email not found in Id-Token");
            }

            return email;
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Id-Token", e);
        }
    }

    public static boolean verifyUserRoleFromToken(String idToken, List<String> rolesRequest, String secret) {
        return verifyUserRoleFromToken(idToken, rolesRequest, true, secret);
    }

    public static boolean verifyUserRoleFromToken(String idToken, List<String>  rolesRequest, boolean requireAll, String secret) {
        try {
            Claims claims = decodeToken(idToken, secret);
            String role = extractRole(claims);
            return role != null && rolesRequest.contains(role);

        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Id-Token", e);
        }
    }

    /**
     * Parsea y valida la firma y expiración del token retornando los Claims.
     */
    public static Claims decodeToken(String idToken, String secret) {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("No se encontro Token");
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();

        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Token inválido", e);
        }
    }


    private static String extractRole(Claims claims) {
        Object roleClaim = claims.get("role");

        return Objects.isNull(roleClaim) ? null : roleClaim.toString();
    }
}