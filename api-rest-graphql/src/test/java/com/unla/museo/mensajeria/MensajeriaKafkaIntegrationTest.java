package com.unla.museo.mensajeria;

import com.jayway.jsonpath.JsonPath;
import com.unla.museo.auditoria.HistorialActividadEntity;
import com.unla.museo.auditoria.HistorialActividadRepository;
import com.unla.museo.comun.ServidorGrpcEnProceso;
import com.unla.museo.comun.TokensDePrueba;
import com.unla.museo.notificaciones.NotificacionEntity;
import com.unla.museo.notificaciones.NotificacionRepository;
import com.unla.museo.notificaciones.NotificacionesConsumidor;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import com.unla.museo.usuarios.grpc.UsuariosServiceGrpc;
import com.unla.museo.usuarios.grpc.service.UsuariosGrpcService;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo con un broker Kafka embebido (sin Docker): REST -> outbox ->
 * publicador -> consumidores -> tablas, todo en un mismo proceso. Usa su propia
 * H2 para no compartir (ni vaciar) la de los otros contextos de test.
 *
 * Si EmbeddedKafka no logra borrar sus logs al cerrar en Windows, solo escribe
 * una advertencia: los tests ya terminaron y no fallan por eso.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:museo-kafka-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.kafka.enabled=true",
        "app.kafka.publicador.enabled=true",
        "app.kafka.publicador.intervalo-ms=200",
        "app.kafka.consumidores=notificaciones,auditoria"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 3, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class MensajeriaKafkaIntegrationTest {

    private static final String TOPIC = "eventos.novedades";
    private static final long ESPERA_MS = 40_000;

    /** Los topics reales: 3 particiones el principal, 1 el DLT. */
    @TestConfiguration
    static class Topics {
        @Bean
        NewTopic novedades() {
            return TopicBuilder.name(TOPIC).partitions(3).replicas(1).build();
        }

        @Bean
        NewTopic dlt() {
            return TopicBuilder.name(TOPIC + ".DLT").partitions(1).replicas(1).build();
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private NotificacionRepository notificaciones;
    @Autowired
    private HistorialActividadRepository historial;
    @Autowired
    private MensajeFallidoRepository fallidos;
    @Autowired
    private MensajeProcesadoRepository procesados;
    @Autowired
    private MensajeSalienteRepository salientes;
    @Autowired
    private NotificacionesConsumidor notificacionesConsumidor;
    @Autowired
    private UsuariosGrpcService usuariosGrpcService;
    @Autowired
    private FormatoNovedad formato;
    @Autowired
    @Qualifier("UsuarioSQLRepositoryImpl")
    private UsuarioRepository usuarioRepository;

    private static ServidorGrpcEnProceso servidorUsuarios;
    private static ManagedChannel canalCaido;

    @BeforeAll
    static void canalCaido() {
        // Un canal a un servidor que no existe: toda llamada falla con UNAVAILABLE.
        canalCaido = InProcessChannelBuilder.forName("no-existe-" + UUID.randomUUID()).directExecutor().build();
    }

    @AfterAll
    static void cerrar() throws Exception {
        canalCaido.shutdownNow();
        if (servidorUsuarios != null) {
            servidorUsuarios.close();
        }
    }

    private void usarGrpcReal() throws Exception {
        if (servidorUsuarios == null) {
            servidorUsuarios = new ServidorGrpcEnProceso(usuariosGrpcService);
        }
        ReflectionTestUtils.setField(notificacionesConsumidor, "usuarios",
                UsuariosServiceGrpc.newBlockingStub(servidorUsuarios.canal()));
    }

    private void usarGrpcCaido() {
        ReflectionTestUtils.setField(notificacionesConsumidor, "usuarios", UsuariosServiceGrpc.newBlockingStub(canalCaido));
    }

    private Long idUsuario(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow().getId();
    }

    // Sin Awaitility (no es una dependencia del proyecto): bucle con tope de tiempo.
    private static void esperar(String descripcion, BooleanSupplier condicion) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (System.currentTimeMillis() < limite) {
            if (condicion.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("No ocurrió a tiempo: " + descripcion);
    }

    private String cuerpo(String titulo, LocalDateTime fechaHora, int cupo, String movimiento) {
        return """
                {"titulo":"%s","descripcion":"Descripción","tipo":"TALLER","fechaHora":"%s","duracionMinutos":90,"cupoMaximo":%d,"curadorId":%d,"movimiento":"%s"}
                """.formatted(titulo, fechaHora, cupo, idUsuario("curador@test.com"), movimiento);
    }

    private long crear(String token, String cuerpo) throws Exception {
        String respuesta = mockMvc.perform(post("/api/eventos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private List<NotificacionEntity> notificacionesDe(String email, long idEvento, NovedadTipo tipo) {
        return notificaciones.findAll().stream()
                .filter(n -> n.getIdUsuario().equals(idUsuario(email)) && n.getIdEvento() == idEvento
                        && n.getTipo().equals(tipo.name()))
                .toList();
    }

    private List<HistorialActividadEntity> historialDe(long idEvento) {
        return historial.findAll().stream().filter(h -> h.getIdEvento() == idEvento).toList();
    }

    private void esperarConsistencia(String tokenAdmin) throws Exception {
        esperar("la mensajería consistente", () -> {
            try {
                String json = mockMvc.perform(get("/api/mensajeria/consistencia").header("Authorization", tokenAdmin))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                return JsonPath.<Boolean>read(json, "$.consistente");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Test
    void altaModificacionYCancelacionLlegamAlosConsumidoresYElSistemaQuedaConsistente() throws Exception {
        usarGrpcReal();
        String curador = TokensDePrueba.token(mockMvc, "curador@test.com");
        String admin = TokensDePrueba.token(mockMvc, "admin@test.com");
        String visitante = TokensDePrueba.token(mockMvc, "visitante@test.com");
        String maria = TokensDePrueba.token(mockMvc, "maria.gonzalez@test.com");
        LocalDateTime fecha = LocalDateTime.of(2031, 11, 12, 18, 0);

        // ALTA: avisa a quienes tienen "Impresionismo" entre sus preferidos (por gRPC).
        long id = crear(curador, cuerpo("Taller de luz", fecha, 30, "impresionismo"));
        esperar("la notificación de alta", () -> !notificacionesDe("visitante@test.com", id, NovedadTipo.ALTA).isEmpty());
        assertThat(notificacionesDe("visitante@test.com", id, NovedadTipo.ALTA).get(0).getMensaje())
                .isEqualTo("Nuevo evento sobre impresionismo: Taller de luz, el 12/11 18:00.");
        assertThat(notificacionesDe("maria.gonzalez@test.com", id, NovedadTipo.ALTA)).isEmpty();

        mockMvc.perform(post("/api/eventos/" + id + "/inscripcion").header("Authorization", visitante))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/eventos/" + id + "/inscripcion").header("Authorization", maria))
                .andExpect(status().isCreated());

        // MODIFICACION: solo a los inscriptos.
        mockMvc.perform(put("/api/eventos/" + id).header("Authorization", curador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Taller de luz", fecha.plusMinutes(30), 40, "impresionismo")))
                .andExpect(status().isOk());
        esperar("las notificaciones de modificación", () ->
                !notificacionesDe("visitante@test.com", id, NovedadTipo.MODIFICACION).isEmpty()
                        && !notificacionesDe("maria.gonzalez@test.com", id, NovedadTipo.MODIFICACION).isEmpty());
        assertThat(notificacionesDe("visitante@test.com", id, NovedadTipo.MODIFICACION).get(0).getMensaje())
                .isEqualTo("Cambió Taller de luz: nueva fecha 12/11 18:30, cupo 40.");
        assertThat(notificacionesDe("sofia.lopez@test.com", id, NovedadTipo.MODIFICACION)).isEmpty();

        // CANCELACION: a los inscriptos, aunque el evento ya no exista.
        mockMvc.perform(delete("/api/eventos/" + id).header("Authorization", curador)).andExpect(status().isNoContent());
        esperar("las notificaciones de cancelación", () ->
                !notificacionesDe("visitante@test.com", id, NovedadTipo.CANCELACION).isEmpty()
                        && !notificacionesDe("maria.gonzalez@test.com", id, NovedadTipo.CANCELACION).isEmpty());
        assertThat(notificacionesDe("visitante@test.com", id, NovedadTipo.CANCELACION).get(0).getMensaje())
                .isEqualTo("Se canceló Taller de luz, que era el 12/11 18:30.");

        // Historial: tres filas, con las fotos y los campos modificados.
        esperar("las tres filas de historial", () -> historialDe(id).size() == 3);
        HistorialActividadEntity modificacion = historialDe(id).stream()
                .filter(h -> h.getTipo().equals("MODIFICACION")).findFirst().orElseThrow();
        assertThat(modificacion.getCamposModificados()).isEqualTo("fechaHora,cupoMaximo");
        assertThat(modificacion.getNombreUsuarioResponsable()).isEqualTo("Juan Perez");
        assertThat(modificacion.getDatosAnteriores()).contains("\"cupoMaximo\":30");
        assertThat(modificacion.getDatosPosteriores()).contains("\"cupoMaximo\":40");
        HistorialActividadEntity alta = historialDe(id).stream().filter(h -> h.getTipo().equals("ALTA")).findFirst().orElseThrow();
        assertThat(alta.getDatosAnteriores()).isNull();

        esperarConsistencia(admin);
        String json = mockMvc.perform(get("/api/mensajeria/consistencia").header("Authorization", admin))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<Integer>read(json, "$.pendientesDePublicar")).isZero();
        assertThat(JsonPath.<Integer>read(json, "$.generados")).isEqualTo(JsonPath.<Integer>read(json, "$.publicados"));
    }

    @Test
    void elMismoMensajePublicadoDosVecesNoDuplicaNada() throws Exception {
        Long visitante = idUsuario("visitante@test.com");
        long idEvento = 880_001L;
        String repetido = novedadJson(UUID.randomUUID(), idEvento, "MODIFICACION", 1, visitante);
        String centinela = novedadJson(UUID.randomUUID(), idEvento, "MODIFICACION", 1, visitante);

        // Misma clave = misma partición = se procesan en este orden: cuando el
        // centinela está procesado, el duplicado ya pasó por los dos consumidores.
        kafkaTemplate.send(TOPIC, String.valueOf(idEvento), repetido).get(10, TimeUnit.SECONDS);
        kafkaTemplate.send(TOPIC, String.valueOf(idEvento), repetido).get(10, TimeUnit.SECONDS);
        kafkaTemplate.send(TOPIC, String.valueOf(idEvento), centinela).get(10, TimeUnit.SECONDS);

        esperar("el centinela en ambos consumidores", () -> notificaciones.findAll().stream().anyMatch(
                        n -> n.getIdMensaje().equals(idDe(centinela)))
                && historial.existsById(idDe(centinela)));

        assertThat(notificaciones.findAll().stream().filter(n -> n.getIdMensaje().equals(idDe(repetido)))).hasSize(1);
        assertThat(historial.findAll().stream().filter(h -> h.getIdMensaje().equals(idDe(repetido)))).hasSize(1);
        assertThat(procesados.existsById(new MensajeProcesadoId("notificaciones", idDe(repetido)))).isTrue();
        assertThat(procesados.existsById(new MensajeProcesadoId("auditoria", idDe(repetido)))).isTrue();
    }

    @Test
    void unMensajeInvalidoTerminaEnFallidosSinTocarLasTablasDeLosConsumidores() throws Exception {
        long notificacionesAntes = notificaciones.count();
        long historialAntes = historial.count();
        long procesadosNotificacionesAntes = procesados.contarPorConsumidor("notificaciones");
        long procesadosAuditoriaAntes = procesados.contarPorConsumidor("auditoria");
        String versionDesconocida = novedadJson(UUID.randomUUID(), 880_002L, "MODIFICACION", 2, 1L);

        kafkaTemplate.send(TOPIC, "880002", "esto no es json").get(10, TimeUnit.SECONDS);
        kafkaTemplate.send(TOPIC, "880002", versionDesconocida).get(10, TimeUnit.SECONDS);

        // Cada mensaje lo rechazan los dos consumidores: 2 mensajes x 2 grupos.
        esperar("los cuatro fallidos", () -> fallidos.findAll().stream()
                .filter(f -> f.getEstado() == EstadoFallido.PENDIENTE).count() >= 4);
        List<MensajeFallidoEntity> pendientes = fallidos.findAll().stream()
                .filter(f -> f.getEstado() == EstadoFallido.PENDIENTE).toList();
        try {
            assertThat(pendientes).extracting(MensajeFallidoEntity::getConsumidor)
                    .containsOnly("notificaciones", "auditoria");
            assertThat(pendientes).filteredOn(f -> "esto no es json".equals(f.getContenido()))
                    .hasSize(2).allSatisfy(f -> {
                        assertThat(f.getIdMensaje()).isNull();
                        assertThat(f.getError()).contains("JSON");
                    });
            assertThat(pendientes).filteredOn(f -> versionDesconocida.equals(f.getContenido()))
                    .hasSize(2).allSatisfy(f -> {
                        assertThat(f.getIdMensaje()).isEqualTo(idDe(versionDesconocida));
                        assertThat(f.getError()).contains("Versión de esquema no soportada");
                    });
            assertThat(notificaciones.count()).isEqualTo(notificacionesAntes);
            assertThat(historial.count()).isEqualTo(historialAntes);
            assertThat(procesados.contarPorConsumidor("notificaciones")).isEqualTo(procesadosNotificacionesAntes);
            assertThat(procesados.contarPorConsumidor("auditoria")).isEqualTo(procesadosAuditoriaAntes);
        } finally {
            // Se dan por resueltos para no ensuciar el chequeo de consistencia de los otros tests.
            pendientes.forEach(f -> f.setEstado(EstadoFallido.REPROCESADO));
            fallidos.saveAll(pendientes);
        }
    }

    @Test
    void siElBackendGrpcNoResponderSeReintentaVaAlDltYAlReprocesarSeProcesaUnaSolaVez() throws Exception {
        String curador = TokensDePrueba.token(mockMvc, "curador@test.com");
        String admin = TokensDePrueba.token(mockMvc, "admin@test.com");
        usarGrpcCaido();
        long idCreado;
        try {
            idCreado = crear(curador, cuerpo("Taller que falla", LocalDateTime.of(2031, 12, 1, 10, 0), 20, "Impresionismo"));
            // 3 reintentos con 2 s de espera y después el DLT.
            esperar("el fallido de notificaciones", () -> fallidos.findAll().stream().anyMatch(f ->
                    f.getEstado() == EstadoFallido.PENDIENTE && "notificaciones".equals(f.getConsumidor())
                            && f.getContenido().contains("Taller que falla")));
        } finally {
            usarGrpcReal();
        }
        final long id = idCreado;
        MensajeFallidoEntity fallido = fallidos.findAll().stream().filter(f -> f.getEstado() == EstadoFallido.PENDIENTE
                && f.getContenido().contains("Taller que falla")).findFirst().orElseThrow();
        assertThat(fallido.getError()).isNotBlank();
        assertThat(fallido.getIdMensaje()).isNotNull();
        // Auditoría sí lo procesó: una falla de un consumidor no frena al otro.
        esperar("el historial", () -> historial.existsById(fallido.getIdMensaje()));
        assertThat(notificacionesDe("visitante@test.com", id, NovedadTipo.ALTA)).isEmpty();

        // Un curador no puede reprocesar; el administrador sí.
        mockMvc.perform(post("/api/mensajeria/fallidos/" + fallido.getId() + "/reprocesar")
                        .header("Authorization", curador))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/mensajeria/fallidos/" + fallido.getId() + "/reprocesar")
                        .header("Authorization", admin))
                .andExpect(status().isAccepted());

        esperar("la notificación tras reprocesar", () -> !notificacionesDe("visitante@test.com", id, NovedadTipo.ALTA).isEmpty());
        assertThat(fallidos.findById(fallido.getId()).orElseThrow().getEstado()).isEqualTo(EstadoFallido.REPROCESADO);
        // Auditoría ya lo tenía: lo ignora, no queda un segundo registro.
        esperar("el reproceso en auditoría", () -> procesados.existsById(new MensajeProcesadoId("auditoria", fallido.getIdMensaje())));
        assertThat(historialDe(id)).hasSize(1);
        assertThat(notificacionesDe("visitante@test.com", id, NovedadTipo.ALTA)).hasSize(1);

        esperarConsistencia(admin);
    }

    private static String idDe(String json) {
        return JsonPath.read(json, "$.idMensaje");
    }

    // Novedad mínima válida de una MODIFICACION con un inscripto.
    private static String novedadJson(UUID idMensaje, long idEvento, String tipo, int version, long idInscripto) {
        String foto = """
                {"id":%d,"titulo":"Evento directo","descripcion":"d","tipo":"CHARLA","fechaHora":"2032-01-01T10:00:00","duracionMinutos":60,"cupoMaximo":10,"movimiento":null,"idCurador":1,"nombreCurador":"Juan Perez","idsInscriptos":[%d]}
                """.formatted(idEvento, idInscripto).trim();
        return """
                {"idMensaje":"%s","versionEsquema":%d,"tipo":"%s","idEvento":%d,"tituloEvento":"Evento directo","fechaHoraEvento":"2032-01-01T10:00:00","fechaOperacion":"2031-12-31T12:00:00Z","idUsuarioResponsable":null,"nombreUsuarioResponsable":null,"eventoAnterior":%s,"eventoPosterior":%s,"camposModificados":["cupoMaximo"]}
                """.formatted(idMensaje, version, tipo, idEvento, foto, foto).trim();
    }
}
