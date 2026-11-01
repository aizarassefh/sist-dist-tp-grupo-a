package com.unla.museo.notificaciones;

import com.unla.museo.mensajeria.EventoFoto;
import com.unla.museo.mensajeria.FormatoNovedad;
import com.unla.museo.mensajeria.NovedadMensaje;
import com.unla.museo.mensajeria.ProcesadorIdempotente;
import com.unla.museo.usuarios.grpc.UsuariosServiceGrpc;
import com.unla.museo.usuarios.grpc.Visitante;
import com.unla.museo.usuarios.grpc.VisitantesPorMovimientoRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Genera una notificación por visitante destinatario de cada novedad. */
@Component
@ConditionalOnExpression("${app.kafka.enabled:false} and '${app.kafka.consumidores:}'.contains('notificaciones')")
public class NotificacionesConsumidor {

    static final String CONSUMIDOR = "notificaciones";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final long ESPERA_GRPC_SEGUNDOS = 5;

    // Los eventos no guardan a los interesados: se los pregunta al módulo de
    // usuarios por gRPC en lugar de leer sus tablas.
    @GrpcClient("usuarios")
    private UsuariosServiceGrpc.UsuariosServiceBlockingStub usuarios;

    private final FormatoNovedad formato;
    private final ProcesadorIdempotente procesador;
    private final NotificacionRepository repository;
    private final Clock clock;

    public NotificacionesConsumidor(FormatoNovedad formato, ProcesadorIdempotente procesador,
                                    NotificacionRepository repository, Clock clock) {
        this.formato = formato;
        this.procesador = procesador;
        this.repository = repository;
        this.clock = clock;
    }

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "museo-notificaciones", containerFactory = "novedadesContainerFactory")
    public void consumir(String contenido) {
        NovedadMensaje novedad = formato.leer(contenido);
        procesador.procesar(CONSUMIDOR, novedad.idMensaje().toString(), () -> notificar(novedad));
    }

    private void notificar(NovedadMensaje novedad) {
        // Si el backend gRPC no responde (ALTA), la excepción sale y el mensaje se reintenta.
        Set<Long> destinatarios = destinatarios(novedad);
        if (destinatarios.isEmpty()) {
            return;
        }
        String texto = texto(novedad);
        Instant ahora = Instant.now(clock);
        for (Long idUsuario : destinatarios) {
            NotificacionEntity n = new NotificacionEntity();
            n.setIdMensaje(novedad.idMensaje().toString());
            n.setIdUsuario(idUsuario);
            n.setTipo(novedad.tipo().name());
            n.setIdEvento(novedad.idEvento());
            n.setTituloEvento(novedad.tituloEvento());
            n.setMensaje(texto);
            n.setFechaOperacion(novedad.fechaOperacion());
            n.setFechaCreacion(ahora);
            repository.save(n);
        }
    }

    private Set<Long> destinatarios(NovedadMensaje novedad) {
        return switch (novedad.tipo()) {
            case ALTA -> visitantesInteresados(novedad.eventoPosterior().movimiento());
            case MODIFICACION -> sinRepetidos(novedad.eventoPosterior().idsInscriptos());
            case CANCELACION -> sinRepetidos(novedad.eventoAnterior().idsInscriptos());
        };
    }

    private Set<Long> visitantesInteresados(String movimiento) {
        Set<Long> ids = new LinkedHashSet<>();
        if (movimiento == null || movimiento.isBlank()) {
            return ids;
        }
        var respuesta = usuarios.withDeadlineAfter(ESPERA_GRPC_SEGUNDOS, TimeUnit.SECONDS)
                .listarVisitantesPorMovimiento(VisitantesPorMovimientoRequest.newBuilder().setMovimiento(movimiento).build());
        for (Visitante visitante : respuesta.getVisitantesList()) {
            ids.add(visitante.getId());
        }
        return ids;
    }

    private static Set<Long> sinRepetidos(List<Long> ids) {
        return ids == null ? new LinkedHashSet<>() : new LinkedHashSet<>(ids);
    }

    private static String texto(NovedadMensaje n) {
        return switch (n.tipo()) {
            case ALTA -> "Nuevo evento sobre %s: %s, el %s.".formatted(
                    n.eventoPosterior().movimiento(), n.tituloEvento(), fecha(n.fechaHoraEvento()));
            case MODIFICACION -> "Cambió %s: %s.".formatted(n.tituloEvento(), camposLegibles(n));
            case CANCELACION -> "Se canceló %s, que era el %s.".formatted(n.tituloEvento(), fecha(n.fechaHoraEvento()));
        };
    }

    private static String camposLegibles(NovedadMensaje n) {
        EventoFoto nuevo = n.eventoPosterior();
        return n.camposModificados().stream()
                .map(campo -> switch (campo) {
                    case "titulo" -> "nuevo título";
                    case "descripcion" -> "nueva descripción";
                    case "tipo" -> "nuevo tipo " + nuevo.tipo();
                    case "fechaHora" -> "nueva fecha " + fecha(nuevo.fechaHora());
                    case "duracionMinutos" -> "duración " + nuevo.duracionMinutos() + " min";
                    case "cupoMaximo" -> "cupo " + nuevo.cupoMaximo();
                    case "movimiento" -> nuevo.movimiento() == null ? "sin movimiento" : "movimiento " + nuevo.movimiento();
                    case "idCurador" -> "curador " + nuevo.nombreCurador();
                    default -> campo;
                })
                .reduce((a, b) -> a + ", " + b)
                .orElse("sin cambios visibles");
    }

    private static String fecha(LocalDateTime fechaHora) {
        return FORMATO_FECHA.format(fechaHora);
    }
}
