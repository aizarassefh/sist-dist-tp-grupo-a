package com.unla.museo.eventos.grpc.service;

import com.unla.museo.comun.GrpcRespuestas;
import com.unla.museo.comun.Textos;
import com.unla.museo.eventos.entity.EventoEntity;
import com.unla.museo.eventos.entity.util.ConteoPorEvento;
import com.unla.museo.eventos.grpc.EventosDeUsuarioRequest;
import com.unla.museo.eventos.grpc.EventosPorMovimientoRequest;
import com.unla.museo.eventos.grpc.EventosResponse;
import com.unla.museo.eventos.grpc.EventosServiceGrpc;
import com.unla.museo.eventos.grpc.mapper.EventosGrpcMapper;
import com.unla.museo.eventos.repository.EventoRepository;
import com.unla.museo.eventos.repository.InscripcionRepository;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Transacción de solo lectura a nivel de clase: gRPC corre fuera de un request
// web, y sin transacción leer una relación LAZY tira LazyInitializationException.
@GrpcService
@Transactional(readOnly = true)
public class EventosGrpcService extends EventosServiceGrpc.EventosServiceImplBase {

    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public EventosGrpcService(EventoRepository eventoRepository, InscripcionRepository inscripcionRepository,
                              @Qualifier("UsuarioSQLRepositoryImpl") UsuarioRepository usuarioRepository,
                              Clock clock) {
        this.eventoRepository = eventoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    @Override
    public void listarEventosPorMovimiento(EventosPorMovimientoRequest pedido, StreamObserver<EventosResponse> respuesta) {
        GrpcRespuestas.responder(respuesta, () -> listarEventosPorMovimiento(pedido));
    }

    private EventosResponse listarEventosPorMovimiento(EventosPorMovimientoRequest pedido) {
        String movimiento = Textos.sinAcentos(pedido.getMovimiento());
        if (movimiento.isEmpty()) {
            throw Status.INVALID_ARGUMENT.withDescription("Hay que indicar el movimiento").asRuntimeException();
        }
        LocalDateTime ahora = LocalDateTime.now(clock);

        List<EventoEntity> eventos = eventoRepository.listarConMovimiento().stream()
                .filter(e -> movimiento.equals(Textos.sinAcentos(e.getMovimiento())))
                .filter(e -> !pedido.getSoloFuturos() || e.getFechaHora().isAfter(ahora))
                .toList();
        return armarRespuesta(eventos);
    }

    @Override
    public void listarEventosDeUsuario(EventosDeUsuarioRequest pedido, StreamObserver<EventosResponse> respuesta) {
        GrpcRespuestas.responder(respuesta, () -> listarEventosDeUsuario(pedido));
    }

    private EventosResponse listarEventosDeUsuario(EventosDeUsuarioRequest pedido) {
        String email = pedido.getEmail().trim();
        if (email.isEmpty()) {
            throw Status.INVALID_ARGUMENT.withDescription("Hay que indicar el email del usuario").asRuntimeException();
        }
        if (!usuarioRepository.existsByEmail(email)) {
            throw Status.NOT_FOUND.withDescription("No existe el usuario con email " + email).asRuntimeException();
        }
        return armarRespuesta(inscripcionRepository.buscarEventosDeUsuarioPorEmail(email));
    }

    private EventosResponse armarRespuesta(List<EventoEntity> eventos) {
        EventosResponse.Builder resultado = EventosResponse.newBuilder();
        if (eventos.isEmpty()) {
            return resultado.build();
        }
        Map<Long, Long> inscriptos = inscripcionRepository
                .contarPorEventos(eventos.stream().map(EventoEntity::getId).toList()).stream()
                .collect(Collectors.toMap(ConteoPorEvento::getEventoId, ConteoPorEvento::getCantidad));
        for (EventoEntity evento : eventos) {
            resultado.addEventos(EventosGrpcMapper.aEvento(evento, inscriptos.getOrDefault(evento.getId(), 0L).intValue()));
        }
        return resultado.build();
    }
}
