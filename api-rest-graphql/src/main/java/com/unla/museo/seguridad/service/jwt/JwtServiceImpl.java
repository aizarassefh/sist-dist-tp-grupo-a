package com.unla.museo.seguridad.service.jwt;

import com.unla.museo.seguridad.entity.RolEntity;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.comun.errores.MensajesError;
import com.unla.museo.seguridad.exception.UsuarioNoEncontradoException;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Servicio para la generación y validación de tokens JWT.
 */
@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:3600000}")
    private long accessTokenExpirationMs;

    private final UsuarioRepository usuarioRepository;

    public JwtServiceImpl( UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
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
        RolEntity role = this.getRolForUser(email);
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
     * Obtiene el rol del usuario
     */
    private RolEntity getRolForUser(String email) {

        UsuarioEntity user = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException(MensajesError.Usuario.NO_ENCONRTADO));


        if (user.getRol() == null) {
            throw new IllegalStateException("El usuario no tiene un rol asignado");
        }

        return user.getRol();
    }

    @Override
    public long duracionEnSegundos() {
        return accessTokenExpirationMs / 1000;
    }
}
