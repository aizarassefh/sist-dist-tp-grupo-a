package com.unla.museo.gateway.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS de /api/v2/**. Va como filtro y antes que {@link FiltroJwt}: el
 * preflight del navegador no lleva Authorization y no debe recibir 401, y los
 * 401/403 tienen que salir con los headers CORS para que el front pueda leerlos.
 */
@Configuration
public class ConfiguracionCors {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsDeApiV2(@Value("${FRONTEND_URL:http://localhost:5173}") String urlFront) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(urlFront));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/v2/**", config);
        return new CorsWebFilter(fuente);
    }
}
