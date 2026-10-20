package com.unla.museo.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre la unificación del login (punto 4 de la consigna): email inexistente
 * y contraseña incorrecta tienen que dar exactamente el mismo 401, y el email
 * se normaliza (trim + minúsculas) tanto al registrar como al loguearse.
 * Corre contra H2 (perfil test) con los usuarios de ejemplo de DataInitializer
 * ya cargados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    private static final String CONTRASENIA = "Aa@12345678";
    private static final String MENSAJE_GENERICO = "Email o contraseña incorrectos";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginConEmailInexistenteDevuelve401ConMensajeGenerico() throws Exception {
        String cuerpo = "{\"email\":\"no-existe@test.com\",\"password\":\"cualquiera\"}";

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_GENERICO));
    }

    @Test
    void loginConContraseniaIncorrectaDevuelveElMismo401YMensaje() throws Exception {
        String cuerpo = "{\"email\":\"visitante@test.com\",\"password\":\"incorrecta\"}";

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_GENERICO));
    }

    @Test
    void registroConEmailEnMayusculasYEspaciosPermiteLoguearseEnMinuscula() throws Exception {
        String emailConEspacios = "  Nueva.Persona@Test.com  ";
        String cuerpoRegistro = ("{\"email\":\"%s\",\"nombre\":\"Nueva\",\"apellido\":\"Persona\"," +
                "\"telefono\":\"1122334455\",\"password\":\"%s\"}").formatted(emailConEspacios, CONTRASENIA);

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(cuerpoRegistro))
                .andExpect(status().isCreated());

        String cuerpoLoginNormalizado = "{\"email\":\"nueva.persona@test.com\",\"password\":\"%s\"}".formatted(CONTRASENIA);
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpoLoginNormalizado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void loginConEmailEnMayusculasYEspaciosFuncionaParaUsuarioYaRegistrado() throws Exception {
        String cuerpo = "{\"email\":\"  Visitante@Test.com  \",\"password\":\"%s\"}".formatted(CONTRASENIA);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }
}
