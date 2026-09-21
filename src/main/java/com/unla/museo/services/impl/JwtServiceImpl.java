package com.unla.museo.services.impl;

import com.unla.museo.entities.RoleEntity;
import com.unla.museo.entities.UserEntity;
import com.unla.museo.exception.ErrorMessage;
import com.unla.museo.exception.UserNotFoundException;
import com.unla.museo.repositories.UserRepository;
import com.unla.museo.services.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Servicio unificado para gestión de JWT y Refresh Tokens
 * Fusiona la funcionalidad de JwtServiceImpl y RefreshTokenServiceImpl
 */
@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:900000}") // 15 minutos por defecto
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}") // 7 días por defecto
    private long refreshTokenExpirationMs;

    private final UserRepository userRepository;

    public JwtServiceImpl( UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Obtiene la clave de firma para los JWT
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un Access Token JWT con validez corta (15 min)
     */
    @Override
    public String generateAccessToken(String email) {
        RoleEntity role = this.getRoleForUser(email);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("role", role.getId())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Genera un Refresh Token y lo almacena en BD
     */

    /**
     * Parsea y valida el token completo (firma y expiración)
     */
    @Override
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrae el username (subject) del token
     */
    @Override
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extrae la lista de permisos guardados en el claim "permissions"
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        return extractAllClaims(token).get("permissions", List.class);
    }

    /**
     * Verifica si la firma es válida y el token no ha expirado
     */
    @Override
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Obtiene el rol del usuario
     */
    private RoleEntity getRoleForUser(String email) {

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(ErrorMessage.User.NOT_FOUND));


        if (user.getRole() == null ) {
            return null;
        }

        return user.getRole();
    }
}
