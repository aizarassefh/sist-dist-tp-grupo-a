package com.unla.museo.configuration;

import com.unla.museo.controllers.filter.JwtAuthenticationFilter;
import com.unla.museo.controllers.util.LinksApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

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
        .authenticationEntryPoint((request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        })
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
                        // Todas las otras rutas requieren autenticación
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Condición según el ambiente/perfil activo
        if ("local".equalsIgnoreCase(activeProfile) || "dev".equalsIgnoreCase(activeProfile) || "development".equalsIgnoreCase(activeProfile)) {
            configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        } else {
            configuration.setAllowedOrigins(List.of(frontendUrl));
        }

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}