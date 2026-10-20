package com.unla.museo.catalogo;

import com.jayway.jsonpath.JsonPath;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre los casos de la consigna para el catálogo (GraphQL): búsqueda con
 * cada filtro y combinados, paginado, obraEntity(id), seguridad y eficiencia de
 * consultas. Corre contra H2 (perfil test) con las 9 obras de ejemplo que
 * carga CatalogoDataInitializer; ningún test las modifica, así que se
 * pueden leer sin coordinarse entre tests (a diferencia de
 * ReportesIntegrationTest,
 * que sí crea eventos propios).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogoIntegrationTest {

    private static final String CONTRASENIA = "Aa@12345678";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    // ---------- helpers ----------

    private String login(String email) throws Exception {
        String cuerpo = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, CONTRASENIA);
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.accessToken");
    }

    private String cuerpoGraphQl(String query) {
        String escapada = query.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "{\"query\":\"" + escapada + "\"}";
    }

    private MvcResult ejecutarGraphQl(String query, String token) throws Exception {
        var peticion = post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoGraphQl(query));
        if (token != null) {
            peticion = peticion.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(peticion).andExpect(status().isOk()).andReturn();
    }

    private List<String> titulos(String cuerpo) {
        return JsonPath.read(cuerpo, "$.data.obras[*].titulo");
    }

    private Statistics estadisticas() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    // ---------- listado sin filtros ----------

    @Test
    void sinFiltrosDevuelveLasNueveObrasOrdenadasPorTitulo() throws Exception {
        String token = login("curador@test.com");
        MvcResult resultado = ejecutarGraphQl("query { obras { titulo } }", token);
        String cuerpo = resultado.getResponse().getContentAsString();

        assertEquals(List.of(
                "El beso", "El grito", "El nacimiento de Venus", "Guernica", "La Gioconda",
                "La joven de la perla", "La noche estrellada", "La persistencia de la memoria", "Las meninas"),
                titulos(cuerpo));
    }

    // ---------- cada filtro solo ----------

    @Test
    void palabraClaveBuscaEnTitulo() throws Exception {
        String token = login("curador@test.com");
        String query = """
                query { obras(filtro: { palabraClave: "guernica" }) { titulo } }
                """;
        MvcResult resultado = ejecutarGraphQl(query, token);
        assertEquals(List.of("Guernica"), titulos(resultado.getResponse().getContentAsString()));
    }

    @Test
    void palabraClaveBuscaEnDescripcionSinDistinguirMayusculas() throws Exception {
        String token = login("curador@test.com");
        String query = """
                query { obras(filtro: { palabraClave: "SANATORIO" }) { titulo } }
                """;
        MvcResult resultado = ejecutarGraphQl(query, token);
        assertEquals(List.of("La noche estrellada"), titulos(resultado.getResponse().getContentAsString()));
    }

    @Test
    void palabraClaveBuscaEnNombreDelArtista() throws Exception {
        String token = login("curador@test.com");
        String query = """
                query { obras(filtro: { palabraClave: "gogh" }) { titulo } }
                """;
        MvcResult resultado = ejecutarGraphQl(query, token);
        assertEquals(List.of("La noche estrellada"), titulos(resultado.getResponse().getContentAsString()));
    }

    @Test
    void filtroPorEpocaCoincidenciaParcial() throws Exception {
        String token = login("curador@test.com");
        String query = """
                query { obras(filtro: { epoca: "renacimiento" }) { titulo } }
                """;
        MvcResult resultado = ejecutarGraphQl(query, token);
        assertEquals(List.of("El nacimiento de Venus", "La Gioconda"),
                titulos(resultado.getResponse().getContentAsString()));
    }

    @Test
    void filtroPorTecnica() throws Exception {
        String token = login("curador@test.com");
        String query = """
                query { obras(filtro: { tecnica: "temple" }) { titulo } }
                """;
        MvcResult resultado = ejecutarGraphQl(query, token);
        assertEquals(List.of("El nacimiento de Venus"), titulos(resultado.getResponse().getContentAsString()));
    }

    @Test
    void filtroPorUbicacion() throws Exception {
        String token = login("curador@test.com");
        String query = """
                query { obras(filtro: { ubicacion: "depósito" }) { titulo } }
                """;
        MvcResult resultado = ejecutarGraphQl(query, token);
        assertEquals(List.of("El grito"), titulos(resultado.getResponse().getContentAsString()));
    }

    @Test
    void filtroEnExhibicionFalsoDevuelveSoloLasDeDeposito() throws Exception {
        String token = login("curador@test.com");
        String query = """
                query { obras(filtro: { enExhibicion: false }) { titulo } }
                """;
        MvcResult resultado = ejecutarGraphQl(query, token);
        assertEquals(List.of("El grito", "Guernica"), titulos(resultado.getResponse().getContentAsString()));
    }

    // ---------- combinación ----------

    @Test
    void combinaEpocaConEnExhibicion() throws Exception {
        String token = login("curador@test.com");

        String queryTrue = """
                query { obras(filtro: { epoca: "renacimiento", enExhibicion: true }) { titulo } }
                """;
        MvcResult conTrue = ejecutarGraphQl(queryTrue, token);
        assertEquals(List.of("El nacimiento de Venus", "La Gioconda"),
                titulos(conTrue.getResponse().getContentAsString()));

        // Mismo filtro de época, pero en depósito: ninguna de las dos obras del
        // Renacimiento está ahí, así que el AND debe dar lista vacía (si el
        // combinado ignorara enExhibicion, seguiría devolviendo las mismas 2).
        String queryFalse = """
                query { obras(filtro: { epoca: "renacimiento", enExhibicion: false }) { titulo } }
                """;
        MvcResult conFalse = ejecutarGraphQl(queryFalse, token);
        assertEquals(List.of(), titulos(conFalse.getResponse().getContentAsString()));
    }

    // ---------- paginado ----------

    @Test
    void paginadoDevuelvePaginasDeDosElementos() throws Exception {
        String token = login("curador@test.com");

        MvcResult pagina0 = ejecutarGraphQl("query { obras(tamanio: 2, pagina: 0) { titulo } }", token);
        assertEquals(List.of("El beso", "El grito"), titulos(pagina0.getResponse().getContentAsString()));

        MvcResult pagina1 = ejecutarGraphQl("query { obras(tamanio: 2, pagina: 1) { titulo } }", token);
        assertEquals(List.of("El nacimiento de Venus", "Guernica"),
                titulos(pagina1.getResponse().getContentAsString()));
    }

    @Test
    void tamanioFueraDeRangoDevuelveBadRequest() throws Exception {
        String token = login("curador@test.com");
        MvcResult resultado = ejecutarGraphQl("query { obras(tamanio: 500) { titulo } }", token);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertEquals("BAD_REQUEST", JsonPath.read(cuerpo, "$.errors[0].extensions.classification"));
    }

    // ---------- obraEntity(id) ----------

    @Test
    void obraPorIdExistenteTraeArtistaYComentariosConAutor() throws Exception {
        String token = login("curador@test.com");

        String queryListado = """
                query { obras(filtro: { palabraClave: "gioconda" }) { id } }
                """;
        MvcResult listado = ejecutarGraphQl(queryListado, token);
        String id = JsonPath.read(listado.getResponse().getContentAsString(), "$.data.obras[0].id");

        String queryDetalle = """
                query { obra(id: "%s") { titulo artista { nombre } comentarios { usuario texto fecha } } }
                """.formatted(id);
        MvcResult resultado = ejecutarGraphQl(queryDetalle, token);
        String cuerpo = resultado.getResponse().getContentAsString();

        assertEquals("La Gioconda", JsonPath.read(cuerpo, "$.data.obra.titulo"));
        assertEquals("Leonardo da Vinci", JsonPath.read(cuerpo, "$.data.obra.artista.nombre"));
        List<?> comentarios = JsonPath.read(cuerpo, "$.data.obra.comentarios");
        assertEquals(2, comentarios.size());
        assertEquals("Carlos Hernandez", JsonPath.read(cuerpo, "$.data.obra.comentarios[0].usuario"));
        assertEquals("2026-09-01", JsonPath.read(cuerpo, "$.data.obra.comentarios[0].fecha"));
    }

    @Test
    void obraPorIdInexistenteDevuelveNull() throws Exception {
        String token = login("curador@test.com");
        MvcResult resultado = ejecutarGraphQl("query { obra(id: \"999999\") { titulo } }", token);
        assertNull(JsonPath.read(resultado.getResponse().getContentAsString(), "$.data.obra"));
    }

    // ---------- seguridad ----------

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoGraphQl("query { obras { titulo } }")))
                .andExpect(status().isUnauthorized());
    }

    // ---------- eficiencia ----------

    @Test
    void cantidadDeConsultasEsConstanteSinImportarCuantasObrasSePiden() throws Exception {
        String token = login("curador@test.com");
        String queryCompleta = "query { obras(tamanio: %d) { titulo artista { nombre } comentarios { usuario texto } } }";

        Statistics estadisticas = estadisticas();
        estadisticas.clear();
        ejecutarGraphQl(queryCompleta.formatted(2), token);
        long consultasConDosObras = estadisticas.getPrepareStatementCount();
        assertTrue(consultasConDosObras > 0);

        estadisticas.clear();
        ejecutarGraphQl(queryCompleta.formatted(9), token);
        long consultasConNueveObras = estadisticas.getPrepareStatementCount();

        assertEquals(consultasConDosObras, consultasConNueveObras,
                "la cantidad de consultas SQL no debería depender de cuántas obras se piden");

        estadisticas.clear();
        ejecutarGraphQl("query { obras(tamanio: 9) { titulo } }", token);
        long consultasSoloTitulo = estadisticas.getPrepareStatementCount();

        assertTrue(consultasSoloTitulo < consultasConNueveObras,
                "pedir solo 'titulo' no debería consultar artistas ni comentarios");
    }
}
