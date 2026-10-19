package com.unla.museo.seguridad.config;

import com.unla.museo.seguridad.util.LinksApi;
import com.unla.museo.seguridad.service.jwt.JwtAuthenticationFilter;
import com.unla.museo.seguridad.util.Roles;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Value("${app.frontend.url:https://tu-dominio.com}")
    private String frontendUrl;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Habilita la configuración de CORS definida abajo
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas - Documentación
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/graphiql", "/graphiql/**").permitAll()
                        // Rutas públicas - Autenticación
                        .requestMatchers(
                                LinksApi.AuthEndpoints.LOGIN,
                                LinksApi.AuthEndpoints.REGISTER,
                                LinksApi.AuthEndpoints.LOGIN + "/",
                                LinksApi.AuthEndpoints.REGISTER + "/"
                        ).permitAll()
                        // Rutas de gestión: el rol se verifica acá, antes de leer el cuerpo o
                        // los parámetros. Si solo actuara @PreAuthorize (que se evalúa después
                        // de la validación), un visitante con datos inválidos recibiría 400 en
                        // lugar de 403. @PreAuthorize queda como segunda barrera.
                        .requestMatchers(HttpMethod.POST, "/api/eventos").hasAnyRole(Roles.CURADOR, Roles.ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/eventos/*").hasAnyRole(Roles.CURADOR, Roles.ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/eventos/*").hasAnyRole(Roles.CURADOR, Roles.ADMIN)
                        .requestMatchers("/api/reportes/**").hasAnyRole(Roles.CURADOR, Roles.ADMIN)
                        // Todas las otras rutas requieren autenticación
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // Sin token o token inválido: el filtro JWT nunca fija autenticación y esto
    // responde 401 antes de llegar al controlador.
    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
                escribirError(response, HttpStatus.UNAUTHORIZED, "No se encuentra autenticado o el token es inválido o expiró");
    }

    // Respaldo del @ExceptionHandler(AccessDeniedException.class) de
    // GlobalExceptionHandler: este handler solo actúa si Spring Security deniega
    // el acceso fuera del despacho normal de Spring MVC.
    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                escribirError(response, HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta acción");
    }

    // Se arma el JSON a mano (mismo formato que RespuestaError) en lugar de
    // serializar con Jackson: estos handlers corren en el filtro de Spring
    // Security, antes de que exista un HttpMessageConverter, y este proyecto
    // conviven Jackson 2 (jjwt, springdoc) y Jackson 3 (el que autoconfigura
    // Spring Boot 4), así que no hay un único ObjectMapper para inyectar acá.
    // El mensaje siempre es un literal propio, nunca dato externo: no hace
    // falta escapar comillas.
    private void escribirError(HttpServletResponse response, HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String json = "{\"codigo\":%d,\"estado\":\"%s\",\"mensaje\":\"%s\",\"fechaHora\":\"%s\"}"
                .formatted(status.value(), status.name(), mensaje, LocalDateTime.now().format(FORMATO_FECHA_HORA));
        response.getWriter().write(json);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(frontendUrl));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
