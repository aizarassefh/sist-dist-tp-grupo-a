package com.unla.museo.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Base: contexto completo del gateway apuntando a los servidores gRPC falsos. */
@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
abstract class BaseGatewayTest {

    // Mismo default que application.yml y que api-rest-graphql.
    static final String SECRETO = "clave-de-desarrollo-museo-virtual-no-usar-en-produccion";
    static final String EMAIL = "visitante@museo.com";

    @Autowired
    WebTestClient cliente;

    @BeforeEach
    void reiniciarFalsos() {
        ServidoresFalsos.iniciar();
        ServidoresFalsos.reiniciar();
    }

    static String token(String email, String rol) {
        return Jwts.builder().subject(email).claim("role", rol)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(clave(SECRETO)).compact();
    }

    static String tokenVencido() {
        return Jwts.builder().subject(EMAIL).claim("role", "VISITANTE")
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(clave(SECRETO)).compact();
    }

    static String tokenFirmadoConOtraClave() {
        return Jwts.builder().subject(EMAIL).claim("role", "VISITANTE")
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(clave("otra-clave-distinta-de-mas-de-treinta-y-dos-bytes")).compact();
    }

    static String visitante() {
        return "Bearer " + token(EMAIL, "VISITANTE");
    }

    static String curador() {
        return "Bearer " + token("curador@museo.com", "CURADOR");
    }

    private static SecretKey clave(String secreto) {
        return Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }
}
