package com.unla.museo.reportes;

import com.jayway.jsonpath.JsonPath;
import com.unla.museo.eventos.entity.EventoEntity;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.eventos.repository.EventoRepository;
import com.unla.museo.eventos.entity.InscripcionEntity;
import com.unla.museo.eventos.repository.InscripcionRepository;
import com.unla.museo.eventos.util.TipoEvento;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre los casos de la consigna para el reporte de asistencia (GraphQL) y su
 * exportación a Excel. Corre contra H2 (perfil test); cada test crea sus
 * propios eventos en un mes muy lejano (unos 3 años en el futuro, uno por
 * test) para no mezclarse con los datos de ejemplo ni con otros tests, y
 * filtra el reporte y el Excel por ese rango exacto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportesIntegrationTest {

    private static final String CONTRASENIA = "Aa@12345678";

    // Pool de usuarios de ejemplo (DataInitializer) distintos del curador, para
    // poder inscribir a varios sin repetir usuario (la UNIQUE lo impediría).
    private static final List<String> VISITANTES_DE_EJEMPLO = List.of(
            "visitante@test.com", "maria.gonzalez@test.com", "lucia.fernandez@test.com",
            "martin.rodriguez@test.com", "sofia.lopez@test.com", "nicolas.diaz@test.com",
            "valentina.martinez@test.com", "federico.sanchez@test.com", "camila.romero@test.com",
            "admin@test.com");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    @Qualifier("UsuarioSQLRepositoryImpl")
    private UsuarioRepository usuarioRepository;

    @Autowired
    private Clock clock;

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

    private UsuarioEntity usuario(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow();
    }

    private EventoEntity crearEvento(String titulo, LocalDateTime fechaHora, TipoEvento tipo, int cupoMaximo) {
        EventoEntity eventoEntity = new EventoEntity();
        eventoEntity.setTitulo(titulo);
        eventoEntity.setDescripcion("Descripción de prueba para " + titulo);
        eventoEntity.setTipo(tipo);
        eventoEntity.setFechaHora(fechaHora);
        eventoEntity.setDuracionMinutos(60);
        eventoEntity.setCupoMaximo(cupoMaximo);
        eventoEntity.setCurador(usuario("curador@test.com"));
        return eventoRepository.save(eventoEntity);
    }

    private void inscribir(EventoEntity eventoEntity, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            InscripcionEntity inscripcionEntity = new InscripcionEntity();
            inscripcionEntity.setEvento(eventoEntity);
            inscripcionEntity.setUsuario(usuario(VISITANTES_DE_EJEMPLO.get(i)));
            inscripcionEntity.setFechaInscripcion(LocalDateTime.now(clock));
            inscripcionRepository.save(inscripcionEntity);
        }
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

    // ---------- caso calculable de la consigna ----------

    @Test
    void agruparPorMesCalculaCantidadInscriptosYPromedioYPopulares() throws Exception {
        LocalDateTime base = LocalDateTime.now(clock).plusYears(20).withDayOfMonth(1).plusDays(4);
        EventoEntity tallerSinInscriptos = crearEvento("Taller cupo 10 sin inscriptos", base.plusDays(1), TipoEvento.TALLER, 10);
        EventoEntity tallerConDiezInscriptos = crearEvento("Taller cupo 20 con 10 inscriptos", base.plusDays(2), TipoEvento.TALLER, 20);
        inscribir(tallerConDiezInscriptos, 10);

        String desde = base.toLocalDate().withDayOfMonth(1).toString();
        String hasta = base.toLocalDate().withDayOfMonth(base.toLocalDate().lengthOfMonth()).toString();
        String query = """
                query {
                  reporteAsistencia(filtro: { desde: "%s", hasta: "%s", tipo: TALLER, agruparPor: MES }) {
                    fechaCorte
                    grupos { mes tipo cantidadDeEventos totalInscriptosAcumulados promedioDeAsistencia
                      eventosMasPopulares { id titulo cantidadInscriptos } }
                  }
                }
                """.formatted(desde, hasta);

        MvcResult resultado = ejecutarGraphQl(query, login("curador@test.com"));
        String cuerpo = resultado.getResponse().getContentAsString();

        assertEquals(1, ((List<?>) JsonPath.read(cuerpo, "$.data.reporteAsistencia.grupos")).size());
        assertEquals(2, (Integer) JsonPath.read(cuerpo, "$.data.reporteAsistencia.grupos[0].cantidadDeEventos"));
        assertEquals(10, (Integer) JsonPath.read(cuerpo, "$.data.reporteAsistencia.grupos[0].totalInscriptosAcumulados"));
        assertEquals(5.0, ((Number) JsonPath.read(cuerpo, "$.data.reporteAsistencia.grupos[0].promedioDeAsistencia")).doubleValue());
        List<?> populares = JsonPath.read(cuerpo, "$.data.reporteAsistencia.grupos[0].eventosMasPopulares");
        assertEquals(1, populares.size());
        // eventosMasPopulares excluye el de 0 inscriptos (tallerSinInscriptos), solo aparece el de 10.
        // El "id" es GraphQL ID!, que serializa como string.
        assertEquals(tallerConDiezInscriptos.getId().toString(),
                (String) JsonPath.read(cuerpo, "$.data.reporteAsistencia.grupos[0].eventosMasPopulares[0].id"));
    }

    @Test
    void mismoMesDeOtroAnioFormaOtroGrupoAlAgruparPorMes() throws Exception {
        LocalDateTime baseAnioUno = LocalDateTime.now(clock).plusYears(22).withDayOfMonth(10);
        LocalDateTime baseAnioDos = baseAnioUno.plusYears(1);
        crearEvento("Taller año 1", baseAnioUno, TipoEvento.TALLER, 10);
        crearEvento("Taller año 2 mismo mes", baseAnioDos, TipoEvento.TALLER, 10);

        String desde = baseAnioUno.toLocalDate().toString();
        String hasta = baseAnioDos.toLocalDate().toString();
        String query = """
                query {
                  reporteAsistencia(filtro: { desde: "%s", hasta: "%s", tipo: TALLER, agruparPor: MES }) {
                    grupos { mes cantidadDeEventos }
                  }
                }
                """.formatted(desde, hasta);

        MvcResult resultado = ejecutarGraphQl(query, login("curador@test.com"));
        List<?> grupos = JsonPath.read(resultado.getResponse().getContentAsString(), "$.data.reporteAsistencia.grupos");
        assertEquals(2, grupos.size());
    }

    @Test
    void agruparPorTipoYPorMesYTipo() throws Exception {
        LocalDateTime base = LocalDateTime.now(clock).plusYears(25).withDayOfMonth(15);
        crearEvento("Taller para agrupar", base.plusHours(1), TipoEvento.TALLER, 10);
        crearEvento("Charla para agrupar", base.plusHours(2), TipoEvento.CHARLA, 10);
        crearEvento("Visita para agrupar", base.plusHours(3), TipoEvento.VISITA_GUIADA, 10);

        String desde = base.toLocalDate().toString();
        String hasta = base.toLocalDate().toString();

        String consultaPorTipo = """
                query { reporteAsistencia(filtro: { desde: "%s", hasta: "%s", agruparPor: TIPO }) { grupos { tipo cantidadDeEventos } } }
                """.formatted(desde, hasta);
        MvcResult resultadoPorTipo = ejecutarGraphQl(consultaPorTipo, login("curador@test.com"));
        List<?> gruposPorTipo = JsonPath.read(resultadoPorTipo.getResponse().getContentAsString(),
                "$.data.reporteAsistencia.grupos");
        assertEquals(3, gruposPorTipo.size());

        String consultaMesYTipo = """
                query { reporteAsistencia(filtro: { desde: "%s", hasta: "%s", agruparPor: MES_Y_TIPO }) { grupos { mes tipo cantidadDeEventos } } }
                """.formatted(desde, hasta);
        MvcResult resultadoMesYTipo = ejecutarGraphQl(consultaMesYTipo, login("curador@test.com"));
        List<?> gruposMesYTipo = JsonPath.read(resultadoMesYTipo.getResponse().getContentAsString(),
                "$.data.reporteAsistencia.grupos");
        assertEquals(3, gruposMesYTipo.size());
    }

    // ---------- seguridad ----------

    @Test
    void graphqlVisitanteRecibeForbiddenYObrasEnLaMismaOperacion() throws Exception {
        String token = login("visitante@test.com");
        String query = """
                query {
                  obras(tamanio: 1) { titulo }
                  reporteAsistencia(filtro: { agruparPor: MES }) { fechaCorte }
                }
                """;

        MvcResult resultado = ejecutarGraphQl(query, token);
        String cuerpo = resultado.getResponse().getContentAsString();

        assertNull(JsonPath.read(cuerpo, "$.data.reporteAsistencia"));
        assertEquals("FORBIDDEN", JsonPath.read(cuerpo, "$.errors[0].extensions.classification"));
        assertTrue(((List<?>) JsonPath.read(cuerpo, "$.data.obras")).size() >= 1);
    }

    @Test
    void graphqlCuradorRecibeDatos() throws Exception {
        String token = login("curador@test.com");
        String query = """
                query { reporteAsistencia(filtro: { agruparPor: MES }) { fechaCorte grupos { mes } } }
                """;

        MvcResult resultado = ejecutarGraphQl(query, token);
        assertTrue(resultado.getResponse().getContentAsString().contains("fechaCorte"));
    }

    @Test
    void graphqlDesdeMayorQueHastaDevuelveBadRequest() throws Exception {
        String token = login("curador@test.com");
        String query = """
                query { reporteAsistencia(filtro: { desde: "2030-12-31", hasta: "2030-01-01" }) { fechaCorte } }
                """;

        MvcResult resultado = ejecutarGraphQl(query, token);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertEquals("BAD_REQUEST", JsonPath.read(cuerpo, "$.errors[0].extensions.classification"));
    }

    @Test
    void excelSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/reportes/asistencia/excel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void excelVisitanteDevuelve403() throws Exception {
        String token = login("visitante@test.com");
        mockMvc.perform(get("/api/reportes/asistencia/excel").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void excelDesdeMayorQueHastaDevuelve400() throws Exception {
        String token = login("curador@test.com");
        mockMvc.perform(get("/api/reportes/asistencia/excel")
                        .header("Authorization", "Bearer " + token)
                        .param("desde", "2030-12-31")
                        .param("hasta", "2030-01-01"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Excel: contenido ----------

    @Test
    void excelCuradorDevuelve200ConHojasPorTipoYPorcentajesCorrectos() throws Exception {
        LocalDateTime base = LocalDateTime.now(clock).plusYears(28).withDayOfMonth(20);
        EventoEntity tallerSinInscriptos = crearEvento("Excel taller sin inscriptos", base.plusDays(1), TipoEvento.TALLER, 10);
        EventoEntity tallerConDiezInscriptos = crearEvento("Excel taller con diez", base.plusDays(2), TipoEvento.TALLER, 20);
        inscribir(tallerConDiezInscriptos, 10);

        String token = login("curador@test.com");
        MvcResult resultado = mockMvc.perform(get("/api/reportes/asistencia/excel")
                        .header("Authorization", "Bearer " + token)
                        .param("desde", base.toLocalDate().withDayOfMonth(1).toString())
                        .param("hasta", base.toLocalDate().withDayOfMonth(base.toLocalDate().lengthOfMonth()).toString()))
                .andExpect(status().isOk())
                .andReturn();

        byte[] contenido = resultado.getResponse().getContentAsByteArray();
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                resultado.getResponse().getContentType());
        assertTrue(resultado.getResponse().getHeader("Content-Disposition").contains("reporte-asistencia.xlsx"));

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
            Sheet talleres = libro.getSheet("TALLERES");
            assertEquals("Fecha", talleres.getRow(0).getCell(0).getStringCellValue());
            Row fila1 = talleres.getRow(1);
            Row fila2 = talleres.getRow(2);

            assertEquals(CellType.NUMERIC, fila1.getCell(0).getCellType());
            assertTrue(DateUtil.isCellDateFormatted(fila1.getCell(0)));
            assertEquals(0.0, fila1.getCell(5).getNumericCellValue(), 0.0001);
            assertEquals(0.5, fila2.getCell(5).getNumericCellValue(), 0.0001);
            assertEquals(CellType.NUMERIC, fila1.getCell(3).getCellType());
            assertEquals(0.0, fila1.getCell(3).getNumericCellValue(), 0.0001);
            assertEquals(10.0, fila2.getCell(3).getNumericCellValue(), 0.0001);

            Sheet visitas = libro.getSheet("VISITAS GUIADAS");
            Sheet charlas = libro.getSheet("CHARLAS");
            assertEquals("Fecha", visitas.getRow(0).getCell(0).getStringCellValue());
            assertNull(visitas.getRow(1));
            assertEquals("Fecha", charlas.getRow(0).getCell(0).getStringCellValue());
            assertNull(charlas.getRow(1));
        }
    }
}
