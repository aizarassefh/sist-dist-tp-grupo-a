package com.unla.museo.eventos.grpc.service;

import com.unla.museo.eventos.entity.EventoEntity;
import com.unla.museo.eventos.entity.InscripcionEntity;
import com.unla.museo.eventos.grpc.*;
import com.unla.museo.eventos.repository.EventoRepository;
import com.unla.museo.eventos.repository.InscripcionRepository;
import com.unla.museo.eventos.util.EstadoEvento;
import com.unla.museo.eventos.util.TipoEvento;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio gRPC para Gestión de Eventos e Inscripciones.
 * Permite consultar eventos, crear inscripciones y generar reportes de asistencia.
 */
@GrpcService
public class EventosGrpcService extends EventosServiceGrpc.EventosServiceImplBase {

    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public EventosGrpcService(EventoRepository eventoRepository,
                               InscripcionRepository inscripcionRepository) {
        this.eventoRepository = eventoRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    @Override
    public void listarEventos(ListarEventosRequest request,
                              StreamObserver<ListarEventosResponse> responseObserver) {

        int pagina = request.getPagina() > 0 ? request.getPagina() : 0;
        int tamano = request.getTamano() > 0 ? request.getTamano() : 10;

        List<EventoEntity> eventos = eventoRepository.findAll();

        // Filtrar por tipo si se especifica
        if (!request.getTipo().isEmpty()) {
            try {
                TipoEvento tipo = TipoEvento.valueOf(request.getTipo().toUpperCase());
                eventos = eventos.stream()
                    .filter(e -> e.getTipo() == tipo)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Tipo inválido, se ignora el filtro
            }
        }

        // Filtrar por activo
        if (request.getSoloActivos()) {
            eventos = eventos.stream()
                .filter(EventoEntity::isActivo)
                .collect(Collectors.toList());
        }

        // Paginar
        int total = eventos.size();
        List<Evento> eventosPaginados = eventos.stream()
            .skip((long) pagina * tamano)
            .limit(tamano)
            .map(this::convertToEventoProto)
            .collect(Collectors.toList());

        ListarEventosResponse response = ListarEventosResponse.newBuilder()
            .addAllEventos(eventosPaginados)
            .setTotal(total)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void obtenerEvento(ObtenerEventoRequest request,
                              StreamObserver<Evento> responseObserver) {
        Optional<EventoEntity> eventoOpt = eventoRepository.findById(request.getId());

        if (eventoOpt.isPresent()) {
            Evento evento = convertToEventoProto(eventoOpt.get());
            responseObserver.onNext(evento);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription("Evento no encontrado: " + request.getId())
                    .asRuntimeException()
            );
        }
    }

    @Override
    public void crearEvento(CrearEventoRequest request,
                            StreamObserver<Evento> responseObserver) {
        EventoEntity evento = new EventoEntity();
        evento.setTitulo(request.getTitulo());
        evento.setDescripcion(request.getDescripcion());

        try {
            evento.setTipo(TipoEvento.valueOf(request.getTipo().toUpperCase()));
        } catch (IllegalArgumentException e) {
            evento.setTipo(TipoEvento.VISITA_GUIADA); // Default
        }

        // Parsear fechas
        try {
            if (!request.getFechaInicio().isEmpty()) {
                evento.setFechaInicio(LocalDateTime.parse(request.getFechaInicio()));
            }
            if (!request.getFechaFin().isEmpty()) {
                evento.setFechaFin(LocalDateTime.parse(request.getFechaFin()));
            }
        } catch (Exception e) {
            evento.setFechaInicio(LocalDateTime.now());
            evento.setFechaFin(LocalDateTime.now().plusHours(2));
        }

        evento.setUbicacion(request.getUbicacion());
        evento.setCapacidadMaxima(request.getCapacidadMaxima());
        evento.setActivo(true);
        evento.setEstado(EstadoEvento.PLANIFICADO);

        EventoEntity guardado = eventoRepository.save(evento);

        Evento response = convertToEventoProto(guardado);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void actualizarEvento(ActualizarEventoRequest request,
                                 StreamObserver<Evento> responseObserver) {
        Optional<EventoEntity> eventoOpt = eventoRepository.findById(request.getId());

        if (eventoOpt.isEmpty()) {
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription("Evento no encontrado: " + request.getId())
                    .asRuntimeException()
            );
            return;
        }

        EventoEntity evento = eventoOpt.get();

        if (!request.getTitulo().isEmpty()) {
            evento.setTitulo(request.getTitulo());
        }
        if (!request.getDescripcion().isEmpty()) {
            evento.setDescripcion(request.getDescripcion());
        }
        if (!request.getTipo().isEmpty()) {
            try {
                evento.setTipo(TipoEvento.valueOf(request.getTipo().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Tipo inválido, se mantiene el actual
            }
        }
        if (!request.getUbicacion().isEmpty()) {
            evento.setUbicacion(request.getUbicacion());
        }
        if (request.getCapacidadMaxima() > 0) {
            evento.setCapacidadMaxima(request.getCapacidadMaxima());
        }

        EventoEntity actualizado = eventoRepository.save(evento);

        Evento response = convertToEventoProto(actualizado);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void inscribirUsuario(InscribirUsuarioRequest request,
                                 StreamObserver<Inscripcion> responseObserver) {
        Optional<EventoEntity> eventoOpt = eventoRepository.findById(request.getEventoId());

        if (eventoOpt.isEmpty()) {
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription("Evento no encontrado: " + request.getEventoId())
                    .asRuntimeException()
            );
            return;
        }

        EventoEntity evento = eventoOpt.get();

        // Verificar capacidad
        if (evento.getInscripcionesActuales() >= evento.getCapacidadMaxima()) {
            responseObserver.onError(
                io.grpc.Status.RESOURCE_EXHAUSTED
                    .withDescription("El evento ha alcanzado su capacidad máxima")
                    .asRuntimeException()
            );
            return;
        }

        // Crear inscripción
        InscripcionEntity inscripcion = new InscripcionEntity();
        inscripcion.setEvento(evento);
        inscripcion.setUsuario(null); // TODO: obtener usuario autenticado
        inscripcion.setFechaInscripcion(LocalDateTime.now());
        inscripcion.setEstado(InscripcionEntity.EstadoInscripcion.INSCRIPTO);
        inscripcion.setAsistencia(false);

        InscripcionEntity guardada = inscripcionRepository.save(inscripcion);

        // Actualizar contador del evento
        evento.setInscripcionesActuales(evento.getInscripcionesActuales() + 1);
        eventoRepository.save(evento);

        Inscripcion response = convertToInscripcionProto(guardada);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void cancelarInscripcion(CancelarInscripcionRequest request,
                                    StreamObserver<InscripcionResponse> responseObserver) {
        Optional<InscripcionEntity> inscripcionOpt = inscripcionRepository.findById(request.getInscripcionId());

        if (inscripcionOpt.isEmpty()) {
            InscripcionResponse response = InscripcionResponse.newBuilder()
                .setExito(false)
                .setMensaje("Inscripción no encontrada")
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        InscripcionEntity inscripcion = inscripcionOpt.get();

        // Marcar como cancelada
        inscripcion.setEstado(InscripcionEntity.EstadoInscripcion.CANCELADO);
        inscripcionRepository.save(inscripcion);

        // Actualizar contador del evento
        EventoEntity evento = inscripcion.getEvento();
        if (evento != null && evento.getInscripcionesActuales() != null && evento.getInscripcionesActuales() > 0) {
            evento.setInscripcionesActuales(evento.getInscripcionesActuales() - 1);
            eventoRepository.save(evento);
        }

        InscripcionResponse response = InscripcionResponse.newBuilder()
            .setExito(true)
            .setMensaje("Inscripción cancelada correctamente")
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listarInscripciones(ListarInscripcionesRequest request,
                                    StreamObserver<ListarInscripcionesResponse> responseObserver) {
        List<InscripcionEntity> inscripciones;

        if (request.getEventoId() > 0) {
            inscripciones = inscripcionRepository.findByEventoIdOrderByFechaInscripcionAsc(request.getEventoId());
        } else {
            inscripciones = inscripcionRepository.findAll();
        }

        List<Inscripcion> inscripcionesProto = inscripciones.stream()
            .map(this::convertToInscripcionProto)
            .collect(Collectors.toList());

        ListarInscripcionesResponse response = ListarInscripcionesResponse.newBuilder()
            .addAllInscripciones(inscripcionesProto)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void generarReporteAsistencia(ReporteRequest request,
                                         StreamObserver<ReporteAsistencia> responseObserver) {
        Optional<EventoEntity> eventoOpt = eventoRepository.findById(request.getEventoId());

        if (eventoOpt.isEmpty()) {
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription("Evento no encontrado: " + request.getEventoId())
                    .asRuntimeException()
            );
            return;
        }

        EventoEntity evento = eventoOpt.get();
        List<InscripcionEntity> inscripciones = inscripcionRepository.findByEventoIdOrderByFechaInscripcionAsc(evento.getId());
        long totalInscritos = inscripciones.size();

        int totalAsistentes = (int) inscripciones.stream()
            .filter(i -> i.getEstado() == InscripcionEntity.EstadoInscripcion.ASISTIO)
            .count();
        double tasaAsistencia = totalInscritos > 0
            ? (totalAsistentes * 100.0) / totalInscritos
            : 0.0;

        ReporteAsistencia response = ReporteAsistencia.newBuilder()
            .setEventoId(evento.getId())
            .setTituloEvento(evento.getTitulo())
            .setTotalInscritos((int) totalInscritos)
            .setTotalAsistentes(totalAsistentes)
            .setTasaAsistencia(tasaAsistencia)
            .setFechaGeneracion(LocalDateTime.now().format(DATE_FORMATTER))
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Métodos auxiliares de conversión
    private Evento convertToEventoProto(EventoEntity entity) {
        Evento.Builder builder = Evento.newBuilder()
            .setId(entity.getId())
            .setTitulo(entity.getTitulo())
            .setDescripcion(entity.getDescripcion() != null ? entity.getDescripcion() : "")
            .setTipo(entity.getTipo() != null ? entity.getTipo().name() : "")
            .setUbicacion(entity.getUbicacion() != null ? entity.getUbicacion() : "")
            .setCapacidadMaxima(entity.getCapacidadMaxima())
            .setActivo(entity.isActivo());

        if (entity.getFechaInicio() != null) {
            builder.setFechaInicio(entity.getFechaInicio().format(DATE_FORMATTER));
        }
        if (entity.getFechaFin() != null) {
            builder.setFechaFin(entity.getFechaFin().format(DATE_FORMATTER));
        }

        builder.setInscripcionesActuales(entity.getInscripcionesActuales() != null
            ? entity.getInscripcionesActuales() : 0);

        return builder.build();
    }

    private Inscripcion convertToInscripcionProto(InscripcionEntity entity) {
        Inscripcion.Builder builder = Inscripcion.newBuilder()
            .setId(entity.getId())
            .setEstado(entity.getEstado() != null ? entity.getEstado().name() : "INSCRIPTO")
            .setAsistencia(entity.isAsistencia());

        if (entity.getEvento() != null) {
            builder.setEventoId(entity.getEvento().getId());
        }
        if (entity.getUsuario() != null) {
            builder.setUsuarioId(entity.getUsuario().getId());
        }
        if (entity.getFechaInscripcion() != null) {
            builder.setFechaInscripcion(entity.getFechaInscripcion().format(DATE_FORMATTER));
        }

        return builder.build();
    }
}
