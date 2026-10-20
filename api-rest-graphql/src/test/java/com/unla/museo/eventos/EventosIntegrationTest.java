package com.unla.museo.eventos;

import com.jayway.jsonpath.JsonPath;
import com.unla.museo.eventos.entity.EventoEntity;
import com.unla.museo.eventos.entity.InscripcionEntity;
import com.unla.museo.eventos.repository.FiltroFavoritoRepository;
import com.unla.museo.eventos.repository.InscripcionRepository;
import com.unla.museo.eventos.util.TipoEvento;
import com.unla.museo.eventos.repository.EventoRepository;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre los casos de la consigna para eventos, inscripciones, filtros
 * favoritos y GET /api/usuarios/curadores. Corre contra H2 (perfil test) con
 * los usuarios y eventos de ejemplo de DataInitializer/EventosDataInitializer
 * ya cargados (app.datos-de-ejemplo=true por defecto); por eso cada test crea
 * sus propios eventos en una ventana de fechas que los datos de ejemplo no
 * pisan (muy lejos en el futuro), en vez de contar filas a ciegas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventosIntegrationTest {

    private static final String CONTRASENIA = "Aa@12345678";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private FiltroFavoritoRepository filtroFavoritoRepository;

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

    private EventoEntity crearEventoDirecto(String titulo, LocalDateTime fechaHora, TipoEvento tipo, int cupoMaximo, UsuarioEntity curador) {
        EventoEntity eventoEntity = new EventoEntity();
        eventoEntity.setTitulo(titulo);
        eventoEntity.setDescripcion("Descripción de prueba para " + titulo);
        eventoEntity.setTipo(tipo);
        eventoEntity.setFechaHora(fechaHora);
        eventoEntity.setDuracionMinutos(60);
        eventoEntity.setCupoMaximo(cupoMaximo);
        eventoEntity.setCurador(curador);
        return eventoRepository.save(eventoEntity);
    }

    private void inscribirDirecto(EventoEntity eventoEntity, UsuarioEntity usuario) {
        InscripcionEntity inscripcionEntity = new InscripcionEntity();
        inscripcionEntity.setEvento(eventoEntity);
        inscripcionEntity.setUsuario(usuario);
        inscripcionEntity.setFechaInscripcion(LocalDateTime.now(clock));
        inscripcionRepository.save(inscripcionEntity);
    }

    private String cuerpoEvento(String titulo, TipoEvento tipo, LocalDateTime fechaHora, int duracion, int cupo, Long curadorId) {
        return """
                {"titulo":"%s","descripcion":"Descripción de prueba","tipo":"%s","fechaHora":"%s","duracionMinutos":%d,"cupoMaximo":%d,"curadorId":%d}
                """.formatted(titulo, tipo, fechaHora.format(ISO), duracion, cupo, curadorId);
    }

    // ---------- roles ----------

    @Test
    void sinTokenAlCrearEventoDevuelve401() throws Exception {
        LocalDateTime futuro = LocalDateTime.now(clock).plusDays(500);
        mockMvc.perform(post("/api/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEvento("Evento sin token", TipoEvento.CHARLA, futuro, 60, 20, usuario("curador@test.com").getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void visitanteCreaEventoDevuelve403() throws Exception {
        String token = login("visitante@test.com");
        LocalDateTime futuro = LocalDateTime.now(clock).plusDays(500);
        mockMvc.perform(post("/api/eventos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEvento("Evento de visitante", TipoEvento.CHARLA, futuro, 60, 20, usuario("curador@test.com").getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void visitanteEditaEventoDevuelve403() throws Exception {
        String token = login("visitante@test.com");
        EventoEntity eventoEntity = crearEventoDirecto("Evento para editar (403)", LocalDateTime.now(clock).plusDays(501),
                TipoEvento.CHARLA, 20, usuario("curador@test.com"));
        mockMvc.perform(put("/api/eventos/" + eventoEntity.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEvento("Editado", TipoEvento.CHARLA, eventoEntity.getFechaHora(), 60, 20, usuario("curador@test.com").getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void visitanteBorraEventoDevuelve403() throws Exception {
        String token = login("visitante@test.com");
        EventoEntity eventoEntity = crearEventoDirecto("Evento para borrar (403)", LocalDateTime.now(clock).plusDays(502),
                TipoEvento.CHARLA, 20, usuario("curador@test.com"));
        mockMvc.perform(delete("/api/eventos/" + eventoEntity.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void curadorCreaEventoDevuelve201ConTodosLosCampos() throws Exception {
        String token = login("curador@test.com");
        Long curadorId = usuario("curador@test.com").getId();
        LocalDateTime futuro = LocalDateTime.now(clock).plusDays(510);

        mockMvc.perform(post("/api/eventos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEvento("Recorrido de prueba", TipoEvento.VISITA_GUIADA, futuro, 90, 15, curadorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.titulo").value("Recorrido de prueba"))
                .andExpect(jsonPath("$.descripcion").value("Descripción de prueba"))
                .andExpect(jsonPath("$.tipo").value("VISITA_GUIADA"))
                .andExpect(jsonPath("$.duracionMinutos").value(90))
                .andExpect(jsonPath("$.cupoMaximo").value(15))
                .andExpect(jsonPath("$.curadorResponsable.id").value(curadorId))
                .andExpect(jsonPath("$.curadorResponsable.nombre").value("Juan Perez"))
                .andExpect(jsonPath("$.cantidadInscriptos").value(0))
                .andExpect(jsonPath("$.inscripto").value(false))
                .andExpect(jsonPath("$.inscriptos").isArray());
    }

    @Test
    void adminPuedeCrearEvento() throws Exception {
        String token = login("admin@test.com");
        Long curadorId = usuario("curador@test.com").getId();
        LocalDateTime futuro = LocalDateTime.now(clock).plusDays(511);

        mockMvc.perform(post("/api/eventos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEvento("Evento creado por admin", TipoEvento.TALLER, futuro, 60, 10, curadorId)))
                .andExpect(status().isCreated());
    }

    @Test
    void curadorIdDeUsuarioQueNoEsCuradorDevuelve400() throws Exception {
        String token = login("curador@test.com");
        Long idVisitante = usuario("visitante@test.com").getId();
        LocalDateTime futuro = LocalDateTime.now(clock).plusDays(512);

        mockMvc.perform(post("/api/eventos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEvento("Evento con curador inválido", TipoEvento.TALLER, futuro, 60, 10, idVisitante)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cupoCeroDevuelve400() throws Exception {
        String token = login("curador@test.com");
        Long curadorId = usuario("curador@test.com").getId();
        LocalDateTime futuro = LocalDateTime.now(clock).plusDays(513);

        mockMvc.perform(post("/api/eventos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEvento("Evento con cupo cero", TipoEvento.TALLER, futuro, 60, 0, curadorId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void faltaTituloDevuelve400() throws Exception {
        String token = login("curador@test.com");
        Long curadorId = usuario("curador@test.com").getId();
        LocalDateTime futuro = LocalDateTime.now(clock).plusDays(514);
        String cuerpo = """
                {"descripcion":"Sin título","tipo":"TALLER","fechaHora":"%s","duracionMinutos":60,"cupoMaximo":10,"curadorId":%d}
                """.formatted(futuro.format(ISO), curadorId);

        mockMvc.perform(post("/api/eventos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    // ---------- listado ----------

    @Test
    void listadoTieneElFormatoDelContratoYOmiteInscriptos() throws Exception {
        String token = login("visitante@test.com");
        LocalDateTime base = LocalDateTime.now(clock).plusDays(2000);
        crearEventoDirecto("Listado - evento A", base.plusHours(1), TipoEvento.TALLER, 10, usuario("curador@test.com"));

        mockMvc.perform(get("/api/eventos")
                        .header("Authorization", "Bearer " + token)
                        .param("desde", base.toLocalDate().toString())
                        .param("hasta", base.toLocalDate().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanio").value(10))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].inscriptos").doesNotExist())
                .andExpect(jsonPath("$.items[0].curadorResponsable.nombre").value("Juan Perez"));
    }

    @Test
    void listadoFiltraPorTipoYOrdenaPorFecha() throws Exception {
        String token = login("visitante@test.com");
        LocalDateTime base = LocalDateTime.now(clock).plusDays(2100);
        EventoEntity primero = crearEventoDirecto("Orden - taller 1", base.plusHours(1), TipoEvento.TALLER, 10, usuario("curador@test.com"));
        crearEventoDirecto("Orden - charla", base.plusHours(2), TipoEvento.CHARLA, 10, usuario("curador@test.com"));
        EventoEntity tercero = crearEventoDirecto("Orden - taller 2", base.plusHours(3), TipoEvento.TALLER, 10, usuario("curador@test.com"));

        mockMvc.perform(get("/api/eventos")
                        .header("Authorization", "Bearer " + token)
                        .param("desde", base.toLocalDate().toString())
                        .param("hasta", base.toLocalDate().plusDays(1).toString())
                        .param("tipo", "TALLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].id").value(primero.getId()))
                .andExpect(jsonPath("$.items[1].id").value(tercero.getId()));
    }

    @Test
    void desdeMayorQueHastaDevuelve400() throws Exception {
        String token = login("visitante@test.com");
        mockMvc.perform(get("/api/eventos")
                        .header("Authorization", "Bearer " + token)
                        .param("desde", "2030-12-31")
                        .param("hasta", "2030-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tamanioFueraDeRangoDevuelve400() throws Exception {
        String token = login("visitante@test.com");
        mockMvc.perform(get("/api/eventos")
                        .header("Authorization", "Bearer " + token)
                        .param("tamanio", "500"))
                .andExpect(status().isBadRequest());
    }

    // ---------- detalle ----------

    @Test
    void detalleCuradorVeInscriptosYVisitanteNo() throws Exception {
        EventoEntity eventoEntity = crearEventoDirecto("Detalle con inscriptos", LocalDateTime.now(clock).plusDays(2200),
                TipoEvento.CHARLA, 10, usuario("curador@test.com"));
        inscribirDirecto(eventoEntity, usuario("visitante@test.com"));

        String tokenCurador = login("curador@test.com");
        mockMvc.perform(get("/api/eventos/" + eventoEntity.getId()).header("Authorization", "Bearer " + tokenCurador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inscriptos").isArray())
                .andExpect(jsonPath("$.inscriptos[0].nombre").value("Carlos Hernandez"));

        String tokenVisitante = login("visitante@test.com");
        mockMvc.perform(get("/api/eventos/" + eventoEntity.getId()).header("Authorization", "Bearer " + tokenVisitante))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inscriptos").doesNotExist())
                .andExpect(jsonPath("$.inscripto").value(true));
    }

    @Test
    void detalleDeEventoInexistenteDevuelve404() throws Exception {
        String token = login("visitante@test.com");
        mockMvc.perform(get("/api/eventos/999999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ---------- inscripción ----------

    @Test
    void inscribirseDevuelve201YRepetirDevuelve409() throws Exception {
        EventoEntity eventoEntity = crearEventoDirecto("Inscripción simple", LocalDateTime.now(clock).plusDays(2300),
                TipoEvento.CHARLA, 5, usuario("curador@test.com"));
        String token = login("visitante@test.com");

        mockMvc.perform(post("/api/eventos/" + eventoEntity.getId() + "/inscripcion").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inscripto").value(true));

        mockMvc.perform(post("/api/eventos/" + eventoEntity.getId() + "/inscripcion").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void inscribirseSinCupoDevuelve409() throws Exception {
        EventoEntity eventoEntity = crearEventoDirecto("Sin cupo", LocalDateTime.now(clock).plusDays(2301),
                TipoEvento.CHARLA, 1, usuario("curador@test.com"));
        inscribirDirecto(eventoEntity, usuario("maria.gonzalez@test.com"));
        String token = login("visitante@test.com");

        mockMvc.perform(post("/api/eventos/" + eventoEntity.getId() + "/inscripcion").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void inscribirseAEventoPasadoDevuelve409() throws Exception {
        EventoEntity eventoEntity = crearEventoDirecto("Evento ya pasado", LocalDateTime.now(clock).minusDays(1),
                TipoEvento.CHARLA, 20, usuario("curador@test.com"));
        String token = login("visitante@test.com");

        mockMvc.perform(post("/api/eventos/" + eventoEntity.getId() + "/inscripcion").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void desinscribirseDevuelve204YRepetirTambien() throws Exception {
        EventoEntity eventoEntity = crearEventoDirecto("Para desinscribirse", LocalDateTime.now(clock).plusDays(2302),
                TipoEvento.CHARLA, 20, usuario("curador@test.com"));
        String token = login("visitante@test.com");
        mockMvc.perform(post("/api/eventos/" + eventoEntity.getId() + "/inscripcion").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/eventos/" + eventoEntity.getId() + "/inscripcion").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/eventos/" + eventoEntity.getId() + "/inscripcion").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // ---------- editar / borrar ----------

    @Test
    void editarBajandoCupoPorDebajoDeLosInscriptosDevuelve409() throws Exception {
        EventoEntity eventoEntity = crearEventoDirecto("Para bajar el cupo", LocalDateTime.now(clock).plusDays(2400),
                TipoEvento.TALLER, 5, usuario("curador@test.com"));
        inscribirDirecto(eventoEntity, usuario("visitante@test.com"));
        inscribirDirecto(eventoEntity, usuario("maria.gonzalez@test.com"));
        inscribirDirecto(eventoEntity, usuario("lucia.fernandez@test.com"));

        String token = login("curador@test.com");
        mockMvc.perform(put("/api/eventos/" + eventoEntity.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEvento("Con menos cupo", TipoEvento.TALLER, eventoEntity.getFechaHora(), 60, 2, usuario("curador@test.com").getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void borrarEventoConInscriptosDevuelve204YLuego404() throws Exception {
        EventoEntity eventoEntity = crearEventoDirecto("Para borrar con inscriptos", LocalDateTime.now(clock).plusDays(2401),
                TipoEvento.TALLER, 5, usuario("curador@test.com"));
        inscribirDirecto(eventoEntity, usuario("visitante@test.com"));
        Long id = eventoEntity.getId();

        String token = login("curador@test.com");
        mockMvc.perform(delete("/api/eventos/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/eventos/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ---------- filtros favoritos ----------

    @Test
    void filtroFavoritoCrearListarEditarYBorrar() throws Exception {
        String token = login("visitante@test.com");
        String cuerpoCreacion = """
                {"nombre":"Talleres de prueba","descripcion":"Para el test","filtros":{"desde":"2030-01-01","hasta":"2030-01-31","tipo":"TALLER","curadorId":null,"estado":"FUTUROS"}}
                """;

        MvcResult creado = mockMvc.perform(post("/api/filtros-favoritos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCreacion))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Talleres de prueba"))
                .andExpect(jsonPath("$.filtros.tipo").value("TALLER"))
                .andExpect(jsonPath("$.filtros.curadorId").doesNotExist())
                .andReturn();

        Long id = ((Number) JsonPath.read(creado.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(get("/api/filtros-favoritos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());

        String cuerpoEdicion = """
                {"nombre":"Talleres editados","descripcion":null,"filtros":{"desde":null,"hasta":null,"tipo":null,"curadorId":null,"estado":"TODOS"}}
                """;
        mockMvc.perform(put("/api/filtros-favoritos/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEdicion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Talleres editados"))
                .andExpect(jsonPath("$.filtros.estado").value("TODOS"));

        // Un usuario distinto no puede tocar el favorito ajeno: 404, no 403.
        String tokenOtroUsuario = login("maria.gonzalez@test.com");
        mockMvc.perform(put("/api/filtros-favoritos/" + id)
                        .header("Authorization", "Bearer " + tokenOtroUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEdicion))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/filtros-favoritos/" + id).header("Authorization", "Bearer " + tokenOtroUsuario))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/filtros-favoritos/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void listarFiltrosFavoritosSoloDevuelveLosPropios() throws Exception {
        String tokenUno = login("nicolas.diaz@test.com");
        String tokenDos = login("sofia.lopez@test.com");
        String cuerpo = """
                {"nombre":"Favorito de Nicolás","descripcion":null,"filtros":null}
                """;
        mockMvc.perform(post("/api/filtros-favoritos")
                        .header("Authorization", "Bearer " + tokenUno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/filtros-favoritos").header("Authorization", "Bearer " + tokenDos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Favorito de Nicolás')]").isEmpty());
    }

    // ---------- curadores ----------

    @Test
    void listarCuradoresNoExponeEmail() throws Exception {
        String token = login("visitante@test.com");
        mockMvc.perform(get("/api/usuarios/curadores").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Juan Perez')]").exists())
                .andExpect(jsonPath("$[?(@.nombre == 'Marta Gómez')]").exists())
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }
}
