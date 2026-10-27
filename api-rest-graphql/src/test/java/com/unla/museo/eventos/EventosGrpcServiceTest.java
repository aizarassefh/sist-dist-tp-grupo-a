package com.unla.museo.eventos;

import com.unla.museo.comun.ServidorGrpcEnProceso;
import com.unla.museo.eventos.grpc.Evento;
import com.unla.museo.eventos.grpc.EventosDeUsuarioRequest;
import com.unla.museo.eventos.grpc.EventosPorMovimientoRequest;
import com.unla.museo.eventos.grpc.EventosServiceGrpc;
import com.unla.museo.eventos.grpc.service.EventosGrpcService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Llama a EventosService por un servidor gRPC en memoria, con los eventos de
 * ejemplo de EventosDataInitializer (fechas relativas al reloj del sistema).
 */
@SpringBootTest
@ActiveProfiles("test")
class EventosGrpcServiceTest {

    @Autowired
    private EventosGrpcService servicio;

    @Autowired
    private Clock clock;

    private ServidorGrpcEnProceso servidor;
    private EventosServiceGrpc.EventosServiceBlockingStub stub;

    @BeforeEach
    void levantarServidor() throws Exception {
        servidor = new ServidorGrpcEnProceso(servicio);
        stub = EventosServiceGrpc.newBlockingStub(servidor.canal());
    }

    @AfterEach
    void bajarServidor() throws Exception {
        servidor.close();
    }

    @Test
    void eventosPorMovimientoVienenOrdenadosPorFechaAscendente() {
        // En minúsculas: la comparación no distingue mayúsculas.
        List<Evento> eventos = stub.listarEventosPorMovimiento(
                EventosPorMovimientoRequest.newBuilder().setMovimiento("barroco").build()).getEventosList();

        assertThat(eventos).extracting(Evento::getTitulo)
                .contains("Recorrido por el arte barroco", "Charla: el claroscuro en el Barroco");
        assertThat(eventos).allMatch(e -> e.getMovimiento().equals("Barroco"));
        assertThat(eventos.stream().map(Evento::getFechaHora).toList()).isSorted();
    }

    @Test
    void soloFuturosDescartaLosEventosPasadosSegunElReloj() {
        EventosPorMovimientoRequest pedido = EventosPorMovimientoRequest.newBuilder()
                .setMovimiento("Barroco").setSoloFuturos(true).build();

        List<Evento> eventos = stub.listarEventosPorMovimiento(pedido).getEventosList();

        LocalDateTime ahora = LocalDateTime.now(clock);
        assertThat(eventos).extracting(Evento::getTitulo)
                .contains("Charla: el claroscuro en el Barroco")
                .doesNotContain("Recorrido por el arte barroco");
        assertThat(eventos).allMatch(e -> LocalDateTime.parse(e.getFechaHora()).isAfter(ahora));
    }

    @Test
    void elEventoIncluyeCupoInscriptosYTipo() {
        Evento evento = stub.listarEventosPorMovimiento(EventosPorMovimientoRequest.newBuilder()
                .setMovimiento("Cubismo").build()).getEventosList().stream()
                .filter(e -> e.getTitulo().equals("Charla: Picasso y el cubismo")).findFirst().orElseThrow();

        assertThat(evento.getTipo()).isEqualTo("CHARLA");
        assertThat(evento.getCupoMaximo()).isEqualTo(20);
        assertThat(evento.getInscriptos()).isEqualTo(2);
        assertThat(evento.getDuracionMinutos()).isEqualTo(60);
        // ISO 8601 local, sin zona: si no parsea como LocalDateTime, el contrato se rompió.
        assertThat(LocalDateTime.parse(evento.getFechaHora())).isNotNull();
    }

    @Test
    void movimientoSinEventosDevuelveListaVaciaYMovimientoEnBlancoEsInvalid() {
        assertThat(stub.listarEventosPorMovimiento(EventosPorMovimientoRequest.newBuilder()
                .setMovimiento("Dadaísmo").build()).getEventosList()).isEmpty();

        assertThatThrownBy(() -> stub.listarEventosPorMovimiento(EventosPorMovimientoRequest.getDefaultInstance()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
    }

    @Test
    void eventosDeUsuarioIncluyeLosInscriptosYAlgunoConMovimiento() {
        List<Evento> eventos = stub.listarEventosDeUsuario(
                EventosDeUsuarioRequest.newBuilder().setEmail("visitante@test.com").build()).getEventosList();

        assertThat(eventos).isNotEmpty();
        assertThat(eventos).extracting(Evento::getTitulo).contains("Recorrido por el arte barroco");
        assertThat(eventos).anyMatch(e -> !e.getMovimiento().isEmpty());
        assertThat(eventos.stream().map(Evento::getFechaHora).toList()).isSorted();
    }

    @Test
    void eventosDeUsuarioInexistenteDevuelveNotFound() {
        assertThatThrownBy(() -> stub.listarEventosDeUsuario(
                EventosDeUsuarioRequest.newBuilder().setEmail("nadie@test.com").build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class, e -> {
                    assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
                    assertThat(e.getStatus().getDescription()).contains("nadie@test.com");
                });
    }
}
