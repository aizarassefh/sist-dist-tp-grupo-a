package com.unla.museo.gateway.seguridad;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unla.museo.gateway.error.RespuestaError;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Valida el JWT del backend (misma clave HMAC, sub = email, claim "role") en
 * /api/v2/** y aplica la tabla de roles de la consigna. El resto de las rutas
 * (el proxy a la Entrega 1) pasa sin tocar: el backend tiene su propia seguridad.
 *
 * La tabla de rutas vive acá a propósito: si una ruta /api/v2/** no está en ella
 * se responde 404 en este filtro. Sin eso, una ruta o método desconocido caería
 * en el proxy /api/** y viajaría al backend.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FiltroJwt implements WebFilter {

    /** Atributo del exchange con el email del token; los controllers lo leen de acá, nunca del cuerpo. */
    public static final String ATRIBUTO_EMAIL = "gateway.emailAutenticado";

    private static final Set<String> SOLO_VISITANTE = Set.of("VISITANTE");
    private static final Set<String> TODOS = Set.of("VISITANTE", "CURADOR", "ADMINISTRADOR");

    private record Regla(HttpMethod metodo, PathPattern ruta, Set<String> roles) {
    }

    private static final PathPatternParser PARSER = new PathPatternParser();

    private static final List<Regla> REGLAS = List.of(
            regla(HttpMethod.POST, "/api/v2/recorridos", SOLO_VISITANTE),
            regla(HttpMethod.GET, "/api/v2/recomendaciones", SOLO_VISITANTE),
            regla(HttpMethod.GET, "/api/v2/preferencias", SOLO_VISITANTE),
            regla(HttpMethod.PUT, "/api/v2/preferencias", SOLO_VISITANTE),
            regla(HttpMethod.POST, "/api/v2/interacciones", SOLO_VISITANTE),
            regla(HttpMethod.POST, "/api/v2/comparaciones", TODOS),
            regla(HttpMethod.GET, "/api/v2/exploraciones/{movimiento}", TODOS),
            regla(HttpMethod.GET, "/api/v2/categorias", TODOS));

    private final SecretKey clave;
    private final ObjectMapper mapper;

    public FiltroJwt(@Value("${jwt.secret}") String secreto, ObjectMapper mapper) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.mapper = mapper;
    }

    private static Regla regla(HttpMethod metodo, String ruta, Set<String> roles) {
        return new Regla(metodo, PARSER.parse(ruta), roles);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var path = exchange.getRequest().getPath().pathWithinApplication();
        String ruta = path.value();
        boolean esApiV2 = ruta.equals("/api/v2") || ruta.startsWith("/api/v2/");
        if (!esApiV2 || HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        String autorizacion = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (autorizacion == null || !autorizacion.startsWith("Bearer ")) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "Falta el token de autenticación");
        }

        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(clave).build()
                    .parseSignedClaims(autorizacion.substring("Bearer ".length()).trim())
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "El token es inválido o está vencido");
        }

        Optional<Regla> regla = REGLAS.stream()
                .filter(r -> r.metodo().equals(exchange.getRequest().getMethod()) && r.ruta().matches(path))
                .findFirst();
        if (regla.isEmpty()) {
            return error(exchange, HttpStatus.NOT_FOUND, "No existe la ruta " + ruta);
        }

        String email = claims.getSubject();
        String rol = rolDe(claims);
        if (email == null || rol == null || !regla.get().roles().contains(rol)) {
            return error(exchange, HttpStatus.FORBIDDEN, "Tu rol no tiene permiso para esta operación");
        }

        exchange.getAttributes().put(ATRIBUTO_EMAIL, email);
        return chain.filter(exchange);
    }

    private String rolDe(Claims claims) {
        Object rol = claims.get("role");
        return rol instanceof String s ? s : null;
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String mensaje) {
        ServerHttpResponse respuesta = exchange.getResponse();
        respuesta.setStatusCode(status);
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] cuerpo = mapper.writeValueAsBytes(RespuestaError.de(status, mensaje));
            return respuesta.writeWith(Mono.just(respuesta.bufferFactory().wrap(cuerpo)));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}
