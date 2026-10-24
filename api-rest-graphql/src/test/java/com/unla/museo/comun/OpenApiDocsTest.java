package com.unla.museo.comun;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre el punto 6 de la consigna: Swagger/OpenAPI 3.0 documenta los
 * endpoints REST de autenticación, eventos y reportes, con el esquema
 * bearerAuth para probar los protegidos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsExponeOpenApi30ConBearerAuthYLasRutasDeLaConsigna() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String cuerpo = resultado.getResponse().getContentAsString();

        assertTrue(cuerpo.contains("\"openapi\":\"3.0"), "Tiene que declarar OpenAPI 3.0");
        assertTrue(cuerpo.contains("\"bearerAuth\""), "Tiene que declarar el securityScheme bearerAuth");

        for (String ruta : new String[]{
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/me",
                "/api/eventos",
                "/api/eventos/{id}/inscripcion",
                "/api/filtros-favoritos",
                "/api/reportes/asistencia/excel"
        }) {
            assertTrue(cuerpo.contains("\"" + ruta + "\""), "Tiene que documentar la ruta " + ruta);
        }
    }
}
