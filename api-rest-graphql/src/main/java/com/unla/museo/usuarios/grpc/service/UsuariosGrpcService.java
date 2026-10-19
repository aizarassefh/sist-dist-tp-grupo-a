package com.unla.museo.usuarios.grpc.service;

import com.unla.museo.catalogo.entity.ObraEntity;
import com.unla.museo.catalogo.repository.ObraRepository;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import com.unla.museo.usuarios.grpc.*;
import com.unla.museo.usuarios.repository.InteraccionUsuarioRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio gRPC para la gestión de Usuarios y Preferencias.
 * Implementa el contrato definido en usuarios.proto
 */
@GrpcService
public class UsuariosGrpcService extends UsuariosServiceGrpc.UsuariosServiceImplBase {

    private final UsuarioRepository usuarioRepository;
    private final InteraccionUsuarioRepository interaccionRepository;
    private final ObraRepository obraRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public UsuariosGrpcService(UsuarioRepository usuarioRepository,
                                InteraccionUsuarioRepository interaccionRepository,
                                ObraRepository obraRepository) {
        this.usuarioRepository = usuarioRepository;
        this.obraRepository = obraRepository;
        this.interaccionRepository = interaccionRepository;
    }

    @Override
    public void obtenerUsuario(ObtenerUsuarioRequest request,
                                StreamObserver<Usuario> responseObserver) {
        Optional<UsuarioEntity> usuarioOpt;

        if (request.getId() > 0) {
            usuarioOpt = usuarioRepository.findById(request.getId());
        } else if (!request.getEmail().isEmpty()) {
            usuarioOpt = usuarioRepository.findByEmail(request.getEmail());
        } else {
            responseObserver.onError(
                io.grpc.Status.INVALID_ARGUMENT
                    .withDescription("Debe proporcionar id o email")
                    .asRuntimeException()
            );
            return;
        }

        if (usuarioOpt.isPresent()) {
            responseObserver.onNext(convertToUsuarioProto(usuarioOpt.get()));
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription("Usuario no encontrado")
                    .asRuntimeException()
            );
        }
    }

    @Override
    public void actualizarPreferencias(ActualizarPreferenciasRequest request,
                                       StreamObserver<Usuario> responseObserver) {
        Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findById(request.getUsuarioId());

        if (usuarioOpt.isEmpty()) {
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription("Usuario no encontrado: " + request.getUsuarioId())
                    .asRuntimeException()
            );
            return;
        }

        UsuarioEntity usuario = usuarioOpt.get();

        // Actualizar preferencias
        usuario.getPreferencias().setEstilosFavoritos(
            request.getEstilosFavoritosList()
        );
        usuario.getPreferencias().setTecnicasFavoritas(
            request.getTecnicasFavoritasList()
        );
        usuario.getPreferencias().setArtistasFavoritos(
            request.getArtistasFavoritosList()
        );
        usuario.getPreferencias().setTemasInteres(
            request.getTemasInteresList()
        );

        usuarioRepository.save(usuario);

        responseObserver.onNext(convertToUsuarioProto(usuario));
        responseObserver.onCompleted();
    }

    @Override
    public void obtenerPreferencias(ObtenerPreferenciasRequest request,
                                    StreamObserver<PreferenciasUsuario> responseObserver) {
        Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findById(request.getUsuarioId());

        if (usuarioOpt.isEmpty()) {
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription("Usuario no encontrado: " + request.getUsuarioId())
                    .asRuntimeException()
            );
            return;
        }

        PreferenciasUsuario preferencias = convertToPreferenciasProto(
            usuarioOpt.get().getPreferencias()
        );

        responseObserver.onNext(preferencias);
        responseObserver.onCompleted();
    }

    @Override
    public void registrarInteraccion(RegistrarInteraccionRequest request,
                                    StreamObserver<InteraccionResponse> responseObserver) {
        try {
            Optional<com.unla.museo.seguridad.entity.UsuarioEntity> usuarioOpt =
                usuarioRepository.findById(request.getUsuarioId());

            Optional<ObraEntity> obraOpt =
                    obraRepository
                    .findById(request.getObraId());

            if (usuarioOpt.isEmpty()) {
                responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                        .withDescription("Usuario no encontrado: " + request.getUsuarioId())
                        .asRuntimeException()
                );
                return;
            }

            com.unla.museo.usuarios.entity.InteraccionUsuario interaccion =
                new com.unla.museo.usuarios.entity.InteraccionUsuario();
            interaccion.setUsuario(usuarioOpt.get());
            if (obraOpt.isPresent()) {
                interaccion.setObra(obraOpt.get());
            }
            interaccion.setTipoInteraccion(
                com.unla.museo.usuarios.entity.InteraccionUsuario.TipoInteraccion
                    .valueOf(request.getTipoInteraccion().toUpperCase())
            );
            interaccion.setFechaInteraccion(LocalDateTime.now());
            interaccion.setDuracionSegundos(request.getDuracionSegundos());

            interaccionRepository.save(interaccion);

            // Incrementar visitas si es una visita
            if (interaccion.getTipoInteraccion() ==
                com.unla.museo.usuarios.entity.InteraccionUsuario.TipoInteraccion.VISITA
                && obraOpt.isPresent()) {
                obraOpt.get().incrementarVisitas();
            }

            InteraccionResponse response = InteraccionResponse.newBuilder()
                .setExito(true)
                .setMensaje("Interacción registrada correctamente")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                io.grpc.Status.INTERNAL
                    .withDescription("Error al registrar interacción: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    @Override
    public void obtenerHistorialVisitas(ObtenerHistorialRequest request,
                                        StreamObserver<HistorialVisitasResponse> responseObserver) {
        List<com.unla.museo.usuarios.entity.InteraccionUsuario> interacciones;

        if (request.getDiasAtras() > 0) {
            LocalDateTime desde = LocalDateTime.now().minusDays(request.getDiasAtras());
            interacciones = interaccionRepository.findByUsuarioIdDesde(request.getUsuarioId(), desde);
        } else {
            interacciones = interaccionRepository.findByUsuarioIdOrderByFechaInteraccionDesc(request.getUsuarioId());
        }

        List<InteraccionUsuario> interaccionesProto = interacciones.stream()
            .map(this::convertToInteraccionProto)
            .collect(Collectors.toList());

        HistorialVisitasResponse response = HistorialVisitasResponse.newBuilder()
            .addAllInteracciones(interaccionesProto)
            .setTotal(interacciones.size())
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void obtenerObrasFavoritas(ObtenerObrasFavoritasRequest request,
                                      StreamObserver<ObrasFavoritasResponse> responseObserver) {
        Optional<com.unla.museo.seguridad.entity.UsuarioEntity> usuarioOpt =
            usuarioRepository.findById(request.getUsuarioId());

        if (usuarioOpt.isEmpty()) {
            ObrasFavoritasResponse response = ObrasFavoritasResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        // Obtener las obras favoritas del usuario
        List<Long> obraIds = interaccionRepository.findObrasFavoritasByUsuarioId(request.getUsuarioId());

        ObrasFavoritasResponse response = ObrasFavoritasResponse.newBuilder()
            .addAllObraIds(obraIds)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Métodos auxiliares de conversión
    private Usuario convertToUsuarioProto(UsuarioEntity entity) {
        Usuario.Builder builder = Usuario.newBuilder()
            .setId(entity.getId())
            .setEmail(entity.getEmail())
            .setNombre(entity.getNombre())
            .setApellido(entity.getApellido())
            .setRol(entity.getRol().getNombre());

        if (entity.getPreferencias() != null) {
            builder.setPreferencias(convertToPreferenciasProto(entity.getPreferencias()));
        }

        if (entity.getFechaRegistro() != null) {
            builder.setFechaRegistro(entity.getFechaRegistro().format(DATE_FORMATTER));
        }

        return builder.build();
    }

    private PreferenciasUsuario convertToPreferenciasProto(
            com.unla.museo.usuarios.entity.PreferenciasUsuario entity) {
        PreferenciasUsuario.Builder builder = PreferenciasUsuario.newBuilder();

        if (entity.getEstilosFavoritos() != null) {
            builder.addAllEstilosFavoritos(entity.getEstilosFavoritos());
        }
        if (entity.getTecnicasFavoritas() != null) {
            builder.addAllTecnicasFavoritas(entity.getTecnicasFavoritas());
        }
        if (entity.getArtistasFavoritos() != null) {
            builder.addAllArtistasFavoritos(entity.getArtistasFavoritos());
        }
        if (entity.getTemasInteres() != null) {
            builder.addAllTemasInteres(entity.getTemasInteres());
        }
        if (entity.getUmbralCalificacion() != null) {
            builder.setUmbralCalificacion(entity.getUmbralCalificacion());
        }

        return builder.build();
    }

    private InteraccionUsuario convertToInteraccionProto(
            com.unla.museo.usuarios.entity.InteraccionUsuario entity) {
        InteraccionUsuario.Builder builder = InteraccionUsuario.newBuilder()
            .setId(entity.getId())
            .setUsuarioId(entity.getUsuario() != null ? entity.getUsuario().getId() : 0L);

        if (entity.getObra() != null) {
            builder.setObraId(entity.getObra().getId());
        }
        if (entity.getTipoInteraccion() != null) {
            builder.setTipoInteraccion(entity.getTipoInteraccion().name());
        }
        if (entity.getFechaInteraccion() != null) {
            builder.setFecha(entity.getFechaInteraccion().format(DATE_FORMATTER));
        }
        if (entity.getDuracionSegundos() != null) {
            builder.setDuracionSegundos(entity.getDuracionSegundos());
        }

        return builder.build();
    }
}
