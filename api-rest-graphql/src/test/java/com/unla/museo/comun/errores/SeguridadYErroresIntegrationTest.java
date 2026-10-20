package com.unla.museo.comun.errores;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica el único mecanismo de autorización (@PreAuthorize +
 * @EnableMethodSecurity) y el formato uniforme de error de
 * GlobalExceptionHandler, de punta a punta contra el filtro de Spring
 * Security real (no mocks). Usa los usuarios que crea DataInitializer contra
 * H2 (perfil test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadYErroresIntegrationTest {

    private static final String CONTRASENIA = "Aa@12345678";
    // Protegida con @PreAuthorize("hasAnyRole('CURADOR','ADMINISTRADOR')").
    // El id no necesita existir: alcanza con que el rol se rechace antes de
    // llegar al servicio.
    private static final String RUTA_DE_CURADOR = "/api/eventos/999999999";

    @Autowired
    private MockMvc mockMvc;

    @Value("${jwt.secret}")
    private String secreto;

    @Test
    void sinTokenDevuelve401ConCuerpoJson() throws Exception {
        mockMvc.perform(delete(RUTA_DE_CURADOR))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").value(401))
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void tokenAlteradoDevuelve401() throws Exception {
        String token = login("admin@test.com");
        // Cambiar el último carácter invalida la firma sin tocar el contenido.
        String alterado = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401));
    }

    @Test
    void tokenVencidoDevuelve401() throws Exception {
        // Firmado con la clave real y con sub y role válidos: solo falla el vencimiento.
        String vencido = Jwts.builder()
                .subject("admin@test.com")
                .claim("role", "ADMINISTRADOR")
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401));
    }

    @Test
    void visitanteEnRutaDeCuradorDevuelve403NoQuinientos() throws Exception {
        String token = login("visitante@test.com");

        mockMvc.perform(delete(RUTA_DE_CURADOR).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403));
    }

    @Test
    void visitanteConDatosInvalidosRecibe403AntesQue400() throws Exception {
        String token = login("visitante@test.com");

        mockMvc.perform(post("/api/eventos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reportes/asistencia/excel?tipo=INEXISTENTE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void rutaInexistenteDevuelve404() throws Exception {
        String token = login("admin@test.com");

        mockMvc.perform(get("/api/esto-no-existe").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(404));
    }

    @Test
    void ningunaRespuestaDeErrorExponeExcepcionesOStackTraces() throws Exception {
        String tokenVisitante = login("visitante@test.com");
        String tokenAdmin = login("admin@test.com");

        MvcResult sinToken = mockMvc.perform(delete(RUTA_DE_CURADOR)).andReturn();
        MvcResult sinPermiso = mockMvc.perform(delete(RUTA_DE_CURADOR)
                        .header("Authorization", "Bearer " + tokenVisitante))
                .andReturn();
        MvcResult rutaInexistente = mockMvc.perform(get("/api/esto-no-existe")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andReturn();

        for (MvcResult resultado : List.of(sinToken, sinPermiso, rutaInexistente)) {
            String cuerpo = resultado.getResponse().getContentAsString();
            assertFalse(cuerpo.contains("Exception"), "el cuerpo no debe nombrar una clase de excepción: " + cuerpo);
            assertFalse(cuerpo.contains("at com."), "el cuerpo no debe incluir un stack trace: " + cuerpo);
        }
    }

    // Se arma el cuerpo a mano y se lee el token con JsonPath (sin ObjectMapper):
    // el proyecto conviven Jackson 2 (jjwt, springdoc) y Jackson 3 (el que
    // autoconfigura Spring Boot 4 por defecto), así que no hay un único bean
    // ObjectMapper para inyectar en el test.
    private String login(String email) throws Exception {
        String cuerpo = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, CONTRASENIA);

        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.accessToken");
    }
}
