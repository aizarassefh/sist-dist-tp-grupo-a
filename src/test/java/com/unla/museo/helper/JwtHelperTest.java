package com.unla.museo.helper;

import com.unla.museo.constants.Roles;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtHelperTest {

    private static final String SECRET = "test-secret-with-at-least-32-characters";

    @Test
    void acceptsAnyMatchingRequiredRole() {
        String token = token(Roles.CURADOR, new Date(System.currentTimeMillis() + 60_000));

        assertTrue(JwtHelper.verifyUserRoleFromToken(token,
                List.of(Roles.ADMIN, Roles.CURADOR), SECRET));
    }

    @Test
    void rejectsNonMatchingRole() {
        String token = token(Roles.VISITANTE, new Date(System.currentTimeMillis() + 60_000));

        assertFalse(JwtHelper.verifyUserRoleFromToken(token,
                List.of(Roles.ADMIN, Roles.CURADOR), SECRET));
    }

    @Test
    void rejectsExpiredToken() {
        String token = token(Roles.CURADOR, new Date(System.currentTimeMillis() - 1_000));

        assertThrows(IllegalArgumentException.class,
                () -> JwtHelper.verifyUserRoleFromToken(token, List.of(Roles.CURADOR), SECRET));
    }

    @Test
    void rejectsTokenWithoutRole() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("user@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();

        assertFalse(JwtHelper.verifyUserRoleFromToken(token, List.of(Roles.CURADOR), SECRET));
    }

    private String token(String role, Date expiration) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("user@test.com")
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(key)
                .compact();
    }
}
