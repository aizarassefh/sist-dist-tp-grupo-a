package com.unla.museo.auditoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.unla.museo.novedades.NovedadEntity;
import com.unla.museo.novedades.NovedadEvento;
import com.unla.museo.novedades.NovedadKafkaListener;
import com.unla.museo.novedades.NovedadRepository;
import com.unla.museo.novedades.NovedadStream;
import com.unla.museo.novedades.NovedadTipo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KafkaAuditoriaYNovedadesIntegrationTest {

    private static final String CONTRASENIA = "Aa@12345678";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditoriaEventoRepository auditoriaRepository;

    @Autowired
    private NovedadRepository novedadRepository;

    @Autowired
    private NovedadStream novedadStream;

    @Autowired
    private MockMvc mockMvc;

    private AuditoriaEventoKafkaListener auditoriaListener;
    private NovedadKafkaListener novedadListener;

    @BeforeEach
    void setUp() {
        auditoriaRepository.deleteAll();
        novedadRepository.deleteAll();
        auditoriaListener = new AuditoriaEventoKafkaListener(objectMapper, auditoriaRepository);
        novedadListener = new NovedadKafkaListener(objectMapper, novedadRepository, novedadStream);
    }

    private String login(String email) throws Exception {
        String cuerpo = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, CONTRASENIA);
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.accessToken");
    }

    @Test
    @DisplayName("Procesa novedades de ALTA, MODIFICACION y CANCELACION en ambos consumidores independientes")
    void procesaOperacionesEnAmbosConsumidores() throws Exception {
        NovedadEvento alta = new NovedadEvento(
                UUID.randomUUID(), NovedadTipo.ALTA, 10L, "Taller de Grabado",
                LocalDateTime.of(2026, 11, 15, 16, 0), Instant.parse("2026-11-01T14:00:00Z"), 2L);
        NovedadEvento modificacion = new NovedadEvento(
                UUID.randomUUID(), NovedadTipo.MODIFICACION, 10L, "Taller de Grabado Actualizado",
                LocalDateTime.of(2026, 11, 16, 17, 0), Instant.parse("2026-11-01T14:05:00Z"), 2L);
        NovedadEvento cancelacion = new NovedadEvento(
                UUID.randomUUID(), NovedadTipo.CANCELACION, 10L, "Taller de Grabado Actualizado",
                LocalDateTime.of(2026, 11, 16, 17, 0), Instant.parse("2026-11-01T14:10:00Z"), 2L);

        for (NovedadEvento ev : List.of(alta, modificacion, cancelacion)) {
            String json = objectMapper.writeValueAsString(ev);
            auditoriaListener.consumir(json);
            novedadListener.consumir(json);
        }

        assertThat(auditoriaRepository.count()).isEqualTo(3);
        assertThat(novedadRepository.count()).isEqualTo(3);

        AuditoriaEventoEntity registroCancelacion = auditoriaRepository.findById(cancelacion.messageId()).orElseThrow();
        assertThat(registroCancelacion.getType()).isEqualTo(NovedadTipo.CANCELACION);
        assertThat(registroCancelacion.getEventId()).isEqualTo(10L);
        assertThat(registroCancelacion.getResponsibleUserId()).isEqualTo(2L);
        assertThat(registroCancelacion.getMessageJson()).contains("Taller de Grabado Actualizado");
    }

    @Test
    @DisplayName("Garantiza idempotencia: recibir el mismo mensaje más de una vez no duplica auditoría ni novedades")
    void idempotenciaAnteMensajesDuplicados() throws Exception {
        UUID messageId = UUID.randomUUID();
        NovedadEvento evento = new NovedadEvento(
                messageId, NovedadTipo.ALTA, 25L, "Visita Guiada Renacimiento",
                LocalDateTime.of(2026, 12, 1, 11, 0), Instant.now(), 1L);
        String payload = objectMapper.writeValueAsString(evento);

        auditoriaListener.consumir(payload);
        auditoriaListener.consumir(payload);
        auditoriaListener.consumir(payload);

        novedadListener.consumir(payload);
        novedadListener.consumir(payload);

        assertThat(auditoriaRepository.count()).isEqualTo(1);
        assertThat(novedadRepository.count()).isEqualTo(1);
        NovedadEntity novedad = novedadRepository.findById(messageId).orElseThrow();
        assertThat(novedad.getTitle()).isEqualTo("Visita Guiada Renacimiento");
    }

    @Test
    @DisplayName("Propaga excepción ante JSON corrupto o mensaje sin messageId para activar reintentos y DLT")
    void propagaErrorAnteMensajeInvalidoOSinId() {
        assertThatThrownBy(() -> auditoriaListener.consumir("{json-invalido"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> novedadListener.consumir("{json-invalido"))
                .isInstanceOf(IllegalArgumentException.class);

        String sinMessageId = """
                {"type":"ALTA","eventId":1,"title":"Evento sin ID","occurredAt":"2026-11-01T15:00:00Z"}
                """;
        assertThatThrownBy(() -> auditoriaListener.consumir(sinMessageId))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> novedadListener.consumir(sinMessageId))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(auditoriaRepository.count()).isZero();
        assertThat(novedadRepository.count()).isZero();
    }

    @Test
    @DisplayName("GET /api/auditoria/eventos permite acceso a CURADOR y devuelve el historial ordenado")
    void consultarHistorialComoCurador() throws Exception {
        String tokenCurador = login("curador@test.com");

        NovedadEvento evento = new NovedadEvento(
                UUID.randomUUID(), NovedadTipo.MODIFICACION, 5L, "Charla Barroco",
                LocalDateTime.of(2026, 11, 20, 18, 30), Instant.parse("2026-11-01T15:00:00Z"), 3L);
        auditoriaListener.consumir(objectMapper.writeValueAsString(evento));

        mockMvc.perform(get("/api/auditoria/eventos")
                        .header("Authorization", "Bearer " + tokenCurador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value(evento.messageId().toString()))
                .andExpect(jsonPath("$[0].type").value("MODIFICACION"))
                .andExpect(jsonPath("$[0].title").value("Charla Barroco"));
    }

    @Test
    @DisplayName("GET /api/auditoria/eventos rechaza con 403 a VISITANTE y con 401 sin token")
    void rechazaHistorialParaVisitanteYSinToken() throws Exception {
        String tokenVisitante = login("visitante@test.com");

        mockMvc.perform(get("/api/auditoria/eventos")
                        .header("Authorization", "Bearer " + tokenVisitante))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/auditoria/eventos"))
                .andExpect(status().isUnauthorized());
    }
}