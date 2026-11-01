package com.unla.museo.mensajeria;

import com.jayway.jsonpath.JsonPath;
import com.unla.museo.comun.TokensDePrueba;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lado productor, sin broker (el perfil test deja Kafka apagado): cada
 * operación sobre un evento deja su novedad en mensajes_salientes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SalidaDeNovedadesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MensajeSalienteRepository salientes;

    @Autowired
    private FormatoNovedad formato;

    @Autowired
    @Qualifier("UsuarioSQLRepositoryImpl")
    private UsuarioRepository usuarioRepository;

    private Long idUsuario(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow().getId();
    }

    private String cuerpo(String titulo, LocalDateTime fechaHora, int cupo, String movimiento) {
        String campoMovimiento = movimiento != null ? ",\"movimiento\":\"%s\"".formatted(movimiento) : "";
        return """
                {"titulo":"%s","descripcion":"Descripción","tipo":"CHARLA","fechaHora":"%s","duracionMinutos":60,"cupoMaximo":%d,"curadorId":%d%s}
                """.formatted(titulo, fechaHora, cupo, idUsuario("curador@test.com"), campoMovimiento);
    }

    private long crear(String token, String cuerpo) throws Exception {
        String respuesta = mockMvc.perform(post("/api/eventos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private List<NovedadMensaje> novedadesDe(long idEvento) {
        return salientes.findAll().stream()
                .filter(m -> m.getClave().equals(String.valueOf(idEvento)))
                .sorted(Comparator.comparing(MensajeSalienteEntity::getFechaCreacion))
                .map(m -> formato.leer(m.getContenido()))
                .toList();
    }

    @Test
    void altaDejaUnaNovedadPendienteConElResponsableDelToken() throws Exception {
        String token = TokensDePrueba.token(mockMvc, "curador@test.com");
        LocalDateTime fecha = LocalDateTime.of(2031, 3, 4, 18, 30);
        long id = crear(token, cuerpo("Alta con novedad", fecha, 25, "Impresionismo"));

        List<MensajeSalienteEntity> filas = salientes.findAll().stream()
                .filter(m -> m.getClave().equals(String.valueOf(id))).toList();
        assertThat(filas).hasSize(1);
        MensajeSalienteEntity fila = filas.get(0);
        assertThat(fila.getTopic()).isEqualTo("eventos.novedades");
        assertThat(fila.getFechaPublicacion()).isNull();
        assertThat(fila.getIntentos()).isZero();

        NovedadMensaje n = formato.leer(fila.getContenido());
        assertThat(n.idMensaje().toString()).isEqualTo(fila.getIdMensaje());
        assertThat(n.versionEsquema()).isEqualTo(1);
        assertThat(n.tipo()).isEqualTo(NovedadTipo.ALTA);
        assertThat(n.idEvento()).isEqualTo(id);
        assertThat(n.tituloEvento()).isEqualTo("Alta con novedad");
        assertThat(n.fechaHoraEvento()).isEqualTo(fecha);
        assertThat(n.fechaOperacion()).isNotNull();
        assertThat(n.idUsuarioResponsable()).isEqualTo(idUsuario("curador@test.com"));
        assertThat(n.nombreUsuarioResponsable()).isEqualTo("Juan Perez");
        assertThat(n.eventoAnterior()).isNull();
        assertThat(n.camposModificados()).isEmpty();
        EventoFoto posterior = n.eventoPosterior();
        assertThat(posterior.id()).isEqualTo(id);
        assertThat(posterior.tipo()).isEqualTo("CHARLA");
        assertThat(posterior.cupoMaximo()).isEqualTo(25);
        assertThat(posterior.movimiento()).isEqualTo("Impresionismo");
        assertThat(posterior.idCurador()).isEqualTo(idUsuario("curador@test.com"));
        assertThat(posterior.nombreCurador()).isEqualTo("Juan Perez");
        assertThat(posterior.idsInscriptos()).isEmpty();
    }

    @Test
    void edicionRegistraCamposModificadosEInscriptosEnAmbasFotos() throws Exception {
        String curador = TokensDePrueba.token(mockMvc, "curador@test.com");
        String visitante = TokensDePrueba.token(mockMvc, "visitante@test.com");
        LocalDateTime fecha = LocalDateTime.of(2031, 4, 5, 10, 0);
        long id = crear(curador, cuerpo("Edición con novedad", fecha, 20, "Barroco"));
        mockMvc.perform(post("/api/eventos/" + id + "/inscripcion").header("Authorization", visitante))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/eventos/" + id).header("Authorization", curador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Edición con novedad", fecha.plusHours(2), 40, "Barroco")))
                .andExpect(status().isOk());

        // Inscribirse no es una modificación del evento: solo hay alta y edición.
        List<NovedadMensaje> novedades = novedadesDe(id);
        assertThat(novedades).extracting(NovedadMensaje::tipo)
                .containsExactly(NovedadTipo.ALTA, NovedadTipo.MODIFICACION);
        NovedadMensaje modificacion = novedades.get(1);
        assertThat(modificacion.camposModificados()).containsExactly("fechaHora", "cupoMaximo");
        assertThat(modificacion.fechaHoraEvento()).isEqualTo(fecha.plusHours(2));
        assertThat(modificacion.eventoAnterior().cupoMaximo()).isEqualTo(20);
        assertThat(modificacion.eventoPosterior().cupoMaximo()).isEqualTo(40);
        assertThat(modificacion.eventoAnterior().fechaHora()).isEqualTo(fecha);
        assertThat(modificacion.eventoAnterior().idsInscriptos()).containsExactly(idUsuario("visitante@test.com"));
        assertThat(modificacion.eventoPosterior().idsInscriptos()).containsExactly(idUsuario("visitante@test.com"));
    }

    @Test
    void edicionSinCambiosNoPublicaNada() throws Exception {
        String curador = TokensDePrueba.token(mockMvc, "curador@test.com");
        LocalDateTime fecha = LocalDateTime.of(2031, 5, 6, 9, 0);
        String mismo = cuerpo("Sin cambios", fecha, 15, null);
        long id = crear(curador, mismo);

        mockMvc.perform(put("/api/eventos/" + id).header("Authorization", curador)
                        .contentType(MediaType.APPLICATION_JSON).content(mismo))
                .andExpect(status().isOk());

        assertThat(novedadesDe(id)).extracting(NovedadMensaje::tipo).containsExactly(NovedadTipo.ALTA);
    }

    @Test
    void modificarCupoEsUnaModificacionConSoloEseCampo() throws Exception {
        String curador = TokensDePrueba.token(mockMvc, "curador@test.com");
        long id = crear(curador, cuerpo("Cupo con novedad", LocalDateTime.of(2031, 6, 7, 16, 0), 10, null));

        mockMvc.perform(patch("/api/eventos/" + id + "/cupo").header("Authorization", curador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cupoMaximo\":12}"))
                .andExpect(status().isOk());

        List<NovedadMensaje> novedades = novedadesDe(id);
        assertThat(novedades).extracting(NovedadMensaje::tipo)
                .containsExactly(NovedadTipo.ALTA, NovedadTipo.MODIFICACION);
        assertThat(novedades.get(1).camposModificados()).containsExactly("cupoMaximo");
        assertThat(novedades.get(1).eventoAnterior().cupoMaximo()).isEqualTo(10);
        assertThat(novedades.get(1).eventoPosterior().cupoMaximo()).isEqualTo(12);
    }

    @Test
    void borrarRegistraLaCancelacionConLosInscriptosQueTeniaElEvento() throws Exception {
        String curador = TokensDePrueba.token(mockMvc, "curador@test.com");
        String visitante = TokensDePrueba.token(mockMvc, "visitante@test.com");
        String maria = TokensDePrueba.token(mockMvc, "maria.gonzalez@test.com");
        long id = crear(curador, cuerpo("Cancelación con novedad", LocalDateTime.of(2031, 7, 8, 11, 0), 10, "Cubismo"));
        mockMvc.perform(post("/api/eventos/" + id + "/inscripcion").header("Authorization", visitante))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/eventos/" + id + "/inscripcion").header("Authorization", maria))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/eventos/" + id).header("Authorization", curador))
                .andExpect(status().isNoContent());

        List<NovedadMensaje> novedades = novedadesDe(id);
        assertThat(novedades).extracting(NovedadMensaje::tipo)
                .containsExactly(NovedadTipo.ALTA, NovedadTipo.CANCELACION);
        NovedadMensaje cancelacion = novedades.get(1);
        assertThat(cancelacion.eventoPosterior()).isNull();
        assertThat(cancelacion.camposModificados()).isEmpty();
        assertThat(cancelacion.tituloEvento()).isEqualTo("Cancelación con novedad");
        assertThat(cancelacion.eventoAnterior().idsInscriptos())
                .containsExactlyInAnyOrder(idUsuario("visitante@test.com"), idUsuario("maria.gonzalez@test.com"));
    }

    @Test
    void siLaOperacionFallaNoQuedaNingunaNovedad() throws Exception {
        String curador = TokensDePrueba.token(mockMvc, "curador@test.com");
        long antes = salientes.count();

        // Curador inexistente: la operación se rechaza antes de guardar nada.
        mockMvc.perform(post("/api/eventos").header("Authorization", curador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Rechazado", LocalDateTime.of(2031, 8, 9, 9, 0), 10, null)
                                .replace("\"curadorId\":" + idUsuario("curador@test.com"), "\"curadorId\":999999")))
                .andExpect(status().isBadRequest());

        assertThat(salientes.count()).isEqualTo(antes);
    }
}
