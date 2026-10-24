package com.unla.museo.seguridad.service.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filtro de autenticación JWT que valida y procesa tokens en cada request.
 *
 * Busca el token en el header Authorization con formato "Bearer <token>".
 * Extrae el rol único del JWT y lo mapea a una autoridad de Spring Security.
 *
 * El parseo (clave de firma, verificación, expiración) vive en un solo lugar,
 * {@link JwtService#extractAllClaims}, para no duplicar esa lógica acá.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        logger.debug("Processing JWT token for endpoint: {}", request.getRequestURI());

        try {
            Claims claims = jwtService.extractAllClaims(token);

            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            if (username == null || username.isBlank() || role == null || role.isBlank()) {
                throw new IllegalArgumentException("JWT does not contain subject and role");
            }

            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(ROLE_PREFIX + role);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, Collections.singletonList(authority));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.debug("JWT validated for user: {} with role: {}", username, role);

        } catch (Exception e) {
            logger.warn("JWT validation failed for request to {}: {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
