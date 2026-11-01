package com.unla.museo.mensajeria;

import com.unla.museo.auditoria.HistorialActividadEntity;
import com.unla.museo.auditoria.HistorialActividadRepository;
import com.unla.museo.comun.TokensDePrueba;
import com.unla.museo.notificaciones.NotificacionEntity;
import com.unla.museo.notificaciones.NotificacionRepository;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Quién ve qué en los endpoints de notificaciones, historial y mensajería. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MensajeriaSeguridadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private NotificacionRepository notificaciones;
    @Autowired
    private HistorialActividadRepository historial;
    @Autowired
    private MensajeFallidoRepository fallidos;
    @Autowired
    @Qualifier("UsuarioSQLRepositoryImpl")
    private UsuarioRepository usuarioRepository;

    private Long idUsuario(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow().getId();
    }

    private NotificacionEntity notificacion(String email, long idEvento, String texto) {
        NotificacionEntity n = new NotificacionEntity();
        n.setIdMensaje(java.util.UUID.randomUUID().toString());
        n.setIdUsuario(idUsuario(email));
        n.setTipo("MODIFICACION");
        n.setIdEvento(idEvento);
        n.setTituloEvento("Evento " + idEvento);
        n.setMensaje(texto);
        n.setFechaOperacion(Instant.parse("2031-01-01T10:00:00Z").plusSeconds(idEvento - 770_000));
        n.setFechaCreacion(Instant.now());
        return notificaciones.save(n);
    }

    @Test
    void elVisitanteSoloVeYMarcaSusPropiasNotificaciones() throws Exception {
        // Ids de evento propios de este test, para no cruzarse con los de otros.
        NotificacionEntity propia1 = notificacion("sofia.lopez@test.com", 770_001, "para sofía 1");
        NotificacionEntity propia2 = notificacion("sofia.lopez@test.com", 770_002, "para sofía 2");
        NotificacionEntity ajena = notificacion("lucia.fernandez@test.com", 770_003, "para lucía");
        String sofia = TokensDePrueba.token(mockMvc, "sofia.lopez@test.com");

        mockMvc.perform(get("/api/notificaciones").header("Authorization", sofia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noLeidas").value(2))
                .andExpect(jsonPath("$.notificaciones", hasSize(2)))
                // Más reciente primero
                .andExpect(jsonPath("$.notificaciones[0].mensaje").value("para sofía 2"))
                .andExpect(jsonPath("$.notificaciones[0].tipo").value("MODIFICACION"))
                .andExpect(jsonPath("$.notificaciones[0].idEvento").value(770_002))
                .andExpect(jsonPath("$.notificaciones[0].leida").value(false))
                .andExpect(jsonPath("$.notificaciones[0].fechaOperacion").value("2031-01-01T10:00:02Z"));

        // Una notificación ajena responde 404, igual que una inexistente.
        mockMvc.perform(patch("/api/notificaciones/" + ajena.getId() + "/leida").header("Authorization", sofia))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/notificaciones/987654321/leida").header("Authorization", sofia))
                .andExpect(status().isNotFound());
        assertThat(notificaciones.findById(ajena.getId()).orElseThrow().isLeida()).isFalse();

        mockMvc.perform(patch("/api/notificaciones/" + propia1.getId() + "/leida").header("Authorization", sofia))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notificaciones?soloNoLeidas=true").header("Authorization", sofia))
                .andExpect(jsonPath("$.noLeidas").value(1))
                .andExpect(jsonPath("$.notificaciones", hasSize(1)))
                .andExpect(jsonPath("$.notificaciones[0].id").value(propia2.getId()));

        mockMvc.perform(patch("/api/notificaciones/leidas").header("Authorization", sofia))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notificaciones").header("Authorization", sofia))
                .andExpect(jsonPath("$.noLeidas").value(0))
                .andExpect(jsonPath("$.notificaciones", hasSize(2)));
        // Marcar todas no toca las de otro usuario.
        assertThat(notificaciones.findById(ajena.getId()).orElseThrow().isLeida()).isFalse();
    }

    @Test
    void notificacionesSonSoloDelVisitante() throws Exception {
        for (String email : new String[]{"curador@test.com", "admin@test.com"}) {
            String token = TokensDePrueba.token(mockMvc, email);
            mockMvc.perform(get("/api/notificaciones").header("Authorization", token)).andExpect(status().isForbidden());
            mockMvc.perform(patch("/api/notificaciones/leidas").header("Authorization", token)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/notificaciones")).andExpect(status().isUnauthorized());
    }

    @Test
    void elVisitanteNoPuedeVerHistorialNiMensajeria() throws Exception {
        String visitante = TokensDePrueba.token(mockMvc, "visitante@test.com");
        mockMvc.perform(get("/api/auditoria/eventos").header("Authorization", visitante)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/mensajeria/consistencia").header("Authorization", visitante)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/mensajeria/fallidos").header("Authorization", visitante)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/mensajeria/fallidos/1/reprocesar").header("Authorization", visitante))
                .andExpect(status().isForbidden());
    }

    @Test
    void historialConFiltrosParaCuradorYAdministrador() throws Exception {
        HistorialActividadEntity h = new HistorialActividadEntity();
        h.setIdMensaje(java.util.UUID.randomUUID().toString());
        h.setTipo("MODIFICACION");
        h.setIdEvento(770_010L);
        h.setTituloEvento("Charla filtrada");
        h.setFechaHoraEvento(LocalDateTime.of(2031, 11, 12, 18, 30));
        h.setFechaOperacion(Instant.parse("2031-11-01T18:02:11.483Z"));
        h.setIdUsuarioResponsable(2L);
        h.setNombreUsuarioResponsable("Juan Perez");
        h.setCamposModificados("fechaHora,cupoMaximo");
        h.setDatosAnteriores("{\"id\":770010,\"cupoMaximo\":30,\"idsInscriptos\":[3,5]}");
        h.setDatosPosteriores("{\"id\":770010,\"cupoMaximo\":40,\"idsInscriptos\":[3,5]}");
        h.setFechaProcesamiento(Instant.parse("2031-11-01T18:02:12Z"));
        historial.save(h);

        for (String email : new String[]{"curador@test.com", "admin@test.com"}) {
            String token = TokensDePrueba.token(mockMvc, email);
            mockMvc.perform(get("/api/auditoria/eventos?idEvento=770010&tipo=MODIFICACION&limite=10")
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].idMensaje").value(h.getIdMensaje()))
                    .andExpect(jsonPath("$[0].tipo").value("MODIFICACION"))
                    .andExpect(jsonPath("$[0].tituloEvento").value("Charla filtrada"))
                    .andExpect(jsonPath("$[0].fechaHoraEvento").value("2031-11-12T18:30:00"))
                    .andExpect(jsonPath("$[0].fechaOperacion").value("2031-11-01T18:02:11Z"))
                    .andExpect(jsonPath("$[0].usuarioResponsable.id").value(2))
                    .andExpect(jsonPath("$[0].usuarioResponsable.nombre").value("Juan Perez"))
                    .andExpect(jsonPath("$[0].camposModificados[0]").value("fechaHora"))
                    .andExpect(jsonPath("$[0].camposModificados[1]").value("cupoMaximo"))
                    .andExpect(jsonPath("$[0].datosAnteriores.cupoMaximo").value(30))
                    .andExpect(jsonPath("$[0].datosPosteriores.idsInscriptos[1]").value(5))
                    .andExpect(jsonPath("$[0].fechaProcesamiento").value("2031-11-01T18:02:12Z"));
            mockMvc.perform(get("/api/auditoria/eventos?idEvento=770010&tipo=ALTA").header("Authorization", token))
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Test
    void consistenciaYFallidosParaGestoresYReprocesarSoloAdministrador() throws Exception {
        MensajeFallidoEntity f = new MensajeFallidoEntity();
        f.setIdMensaje(java.util.UUID.randomUUID().toString());
        f.setConsumidor("auditoria");
        f.setError("falló");
        f.setContenido("{}");
        f.setFechaRecepcion(Instant.now());
        f.setEstado(EstadoFallido.PENDIENTE);
        f = fallidos.save(f);
        String curador = TokensDePrueba.token(mockMvc, "curador@test.com");
        String admin = TokensDePrueba.token(mockMvc, "admin@test.com");

        mockMvc.perform(get("/api/mensajeria/consistencia").header("Authorization", curador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumidores[0].consumidor").value("notificaciones"))
                .andExpect(jsonPath("$.consumidores[1].consumidor").value("auditoria"))
                .andExpect(jsonPath("$.consistente").value(false));
        mockMvc.perform(get("/api/mensajeria/fallidos?estado=PENDIENTE").header("Authorization", curador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + f.getId() + ")].estado").value("PENDIENTE"));
        mockMvc.perform(get("/api/mensajeria/fallidos?estado=INVENTADO").header("Authorization", admin))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/mensajeria/fallidos/" + f.getId() + "/reprocesar").header("Authorization", curador))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/mensajeria/fallidos/987654321/reprocesar").header("Authorization", admin))
                .andExpect(status().isNotFound());
        // El perfil test tiene Kafka apagado: reprocesar no puede publicar y lo dice.
        mockMvc.perform(post("/api/mensajeria/fallidos/" + f.getId() + "/reprocesar").header("Authorization", admin))
                .andExpect(status().isConflict());
        assertThat(fallidos.findById(f.getId()).orElseThrow().getEstado()).isEqualTo(EstadoFallido.PENDIENTE);
        fallidos.deleteById(f.getId());
    }
}
