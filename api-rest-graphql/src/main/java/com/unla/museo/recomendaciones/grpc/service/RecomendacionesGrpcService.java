package com.unla.museo.recomendaciones.grpc.service;

import com.unla.museo.catalogo.entity.ObraEntity;
import com.unla.museo.catalogo.repository.ObraRepository;
import com.unla.museo.recomendaciones.grpc.*;
import com.unla.museo.recomendaciones.grpc.RecomendacionesServiceGrpc;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import com.unla.museo.usuarios.grpc.PreferenciasUsuario;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio gRPC para Recomendaciones Personalizadas.
 * Genera sugerencias basadas en preferencias, historial y tendencias.
 */
@GrpcService
public class RecomendacionesGrpcService extends RecomendacionesServiceGrpc.RecomendacionesServiceImplBase {

    private final ObraRepository obraRepository;
    private final UsuarioRepository usuarioRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public RecomendacionesGrpcService(ObraRepository obraRepository,
                                       UsuarioRepository usuarioRepository) {
        this.obraRepository = obraRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void obtenerRecomendaciones(ObtenerRecomendacionesRequest request,
                                       StreamObserver<RecomendacionesResponse> responseObserver) {
        int cantidad = request.getCantidad() > 0 ? request.getCantidad() : 10;
        Set<Long> excluirIds = new HashSet<>(request.getExcluirObrasIdsList());

        Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findById(request.getUsuarioId());

        List<ObraEntity> candidatas = obraRepository.findAll().stream()
            .filter(o -> !excluirIds.contains(o.getId()))
            .collect(Collectors.toList());

        List<Recomendacion> recomendaciones = new ArrayList<>();
        PreferenciasUsuario preferenciasUsadas = PreferenciasUsuario.getDefaultInstance();

        if (usuarioOpt.isPresent() && usuarioOpt.get().getPreferencias() != null) {
            var prefs = usuarioOpt.get().getPreferencias();
            preferenciasUsadas = convertToPreferenciasProto(prefs);

            // Filtrar y puntuar según preferencias
            for (ObraEntity obra : candidatas) {
                double score = calcularScorePorPreferencias(obra, prefs);
                if (score > 0) {
                    Recomendacion rec = Recomendacion.newBuilder()
                        .setObra(convertToObraProto(obra))
                        .setScore(score)
                        .setRazon(generarRazonPersonal(obra, prefs))
                        .setTipoRecomendacion("PERSONAL")
                        .build();
                    recomendaciones.add(rec);
                }
            }
        } else {
            // Sin preferencias, recomendar por popularidad
            candidatas = candidatas.stream()
                .sorted(Comparator.comparing(ObraEntity::getVisitas).reversed())
                .limit(cantidad)
                .collect(Collectors.toList());

            for (ObraEntity obra : candidatas) {
                Recomendacion rec = Recomendacion.newBuilder()
                    .setObra(convertToObraProto(obra))
                    .setScore(Double.valueOf(obra.getVisitas()) / 100.0)
                    .setRazon("Obra popular en el museo")
                    .setTipoRecomendacion("POPULAR")
                    .build();
                recomendaciones.add(rec);
            }
        }

        // Ordenar por score y limitar
        List<Recomendacion> topRecomendaciones = recomendaciones.stream()
            .sorted(Comparator.comparing(Recomendacion::getScore).reversed())
            .limit(cantidad)
            .collect(Collectors.toList());

        RecomendacionesResponse response = RecomendacionesResponse.newBuilder()
            .addAllRecomendaciones(topRecomendaciones)
            .setPreferenciasUsadas(preferenciasUsadas)
            .setTotalDisponibles(recomendaciones.size())
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void obtenerRecomendacionesColaborativas(ObtenerRecomendacionesColaborativasRequest request,
                                                   StreamObserver<RecomendacionesResponse> responseObserver) {
        int cantidad = request.getCantidad() > 0 ? request.getCantidad() : 10;

        // En producción, aquí se implementaría el algoritmo colaborativo
        // Por ahora, devolvemos obras populares como ejemplo
        List<ObraEntity> populares = obraRepository.findAll().stream()
            .sorted(Comparator.comparing(ObraEntity::getCalificacionPromedio).reversed())
            .limit(cantidad)
            .collect(Collectors.toList());

        List<Recomendacion> recomendaciones = populares.stream()
            .map(obra -> Recomendacion.newBuilder()
                .setObra(convertToObraProto(obra))
                .setScore(obra.getCalificacionPromedio() * 10)
                .setRazon("Usuarios con gustos similares vieron esta obra")
                .setTipoRecomendacion("COLABORATIVA")
                .build())
            .collect(Collectors.toList());

        RecomendacionesResponse response = RecomendacionesResponse.newBuilder()
            .addAllRecomendaciones(recomendaciones)
            .setTotalDisponibles(recomendaciones.size())
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void obtenerObrasPopulares(ObtenerObrasPopularesRequest request,
                                     StreamObserver<ObrasPopularesResponse> responseObserver) {
        int cantidad = request.getCantidad() > 0 ? request.getCantidad() : 10;
        String periodo = request.getPeriodo().isEmpty() ? "mes" : request.getPeriodo();

        // Filtrar por estilo si se especifica
        List<ObraEntity> candidatas;
        if (!request.getFiltroEstilo().isEmpty()) {
            candidatas = obraRepository.findByEstiloContainingIgnoreCase(request.getFiltroEstilo());
        } else {
            candidatas = obraRepository.findAll();
        }

        // Ordenar por visitas y calificación
        List<ObraPopular> populares = candidatas.stream()
            .sorted(Comparator
                .comparing(ObraEntity::getVisitas).reversed()
                .thenComparing(ObraEntity::getCalificacionPromedio, Comparator.reverseOrder()))
            .limit(cantidad)
            .map(o -> ObraPopular.newBuilder()
                .setObra(convertToObraProto(o))
                .setVisitas(o.getVisitas())
                .setCalificacionPromedio(o.getCalificacionPromedio())
                .build())
            .collect(Collectors.toList());

        // Asignar posiciones
        for (int i = 0; i < populares.size(); i++) {
            populares.set(i, populares.get(i).toBuilder()
                .setPosicionRanking(i + 1)
                .build());
        }

        ObrasPopularesResponse response = ObrasPopularesResponse.newBuilder()
            .addAllObras(populares)
            .setPeriodo(periodo)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void obtenerObrasTendencia(ObtenerObrasTendenciaRequest request,
                                     StreamObserver<ObrasTendenciaResponse> responseObserver) {
        int cantidad = request.getCantidad() > 0 ? request.getCantidad() : 10;

        // Algoritmo de tendencia: obras con alta calificación y muchos comentarios recientes
        // Simplificado: combinación de calificación y visitas
        List<ObraTendencia> tendencias = obraRepository.findAll().stream()
            .map(o -> {
                double aumentoSimulado = Math.random() * 100; // Simulación
                int nuevosComentarios = (int) (Math.random() * 50);
                double scoreTendencia = (o.getCalificacionPromedio() * 10)
                    + (o.getVisitas() / 10.0);

                return ObraTendencia.newBuilder()
                    .setObra(convertToObraProto(o))
                    .setVariacionVisitas(aumentoSimulado)
                    .setNuevosComentarios(nuevosComentarios)
                    .setScoreTendencia(scoreTendencia)
                    .build();
            })
            .sorted(Comparator.comparing(ObraTendencia::getScoreTendencia).reversed())
            .limit(cantidad)
            .collect(Collectors.toList());

        String tendenciaCalculada = tendencias.size() > 0
            ? "Obras de " + tendencias.get(0).getObra().getEstilo() + " en auge"
            : "Obras populares";

        ObrasTendenciaResponse response = ObrasTendenciaResponse.newBuilder()
            .addAllObras(tendencias)
            .setTendenciaCalculada(tendenciaCalculada)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void obtenerSugerenciasContextuales(ObtenerSugerenciasContextualesRequest request,
                                             StreamObserver<RecomendacionesResponse> responseObserver) {
        int cantidad = request.getCantidad() > 0 ? request.getCantidad() : 5;
        String tipoSugerencia = request.getTipoSugerencia().toUpperCase();

        Optional<ObraEntity> obraActualOpt = obraRepository.findById(request.getObraActualId());
        if (obraActualOpt.isEmpty()) {
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription("Obra actual no encontrada")
                    .asRuntimeException()
            );
            return;
        }

        ObraEntity obraActual = obraActualOpt.get();
        List<Recomendacion> sugerencias = new ArrayList<>();

        switch (tipoSugerencia) {
            case "SIGUIENTE_OBRA" -> {
                // Sugerir obras mejor calificadas
                sugerencias = obraRepository.findAll().stream()
                    .filter(o -> !o.getId().equals(obraActual.getId()))
                    .sorted(Comparator.comparing(ObraEntity::getCalificacionPromedio).reversed())
                    .limit(cantidad)
                    .map(o -> Recomendacion.newBuilder()
                        .setObra(convertToObraProto(o))
                        .setScore(o.getCalificacionPromedio() * 10)
                        .setRazon("Visita esta obra de alta calificación")
                        .build())
                    .collect(Collectors.toList());
            }
            case "MISMO_ESTILO" -> {
                sugerencias = obraRepository
                    .findByEstiloContainingIgnoreCase(obraActual.getEstilo()).stream()
                    .filter(o -> !o.getId().equals(obraActual.getId()))
                    .limit(cantidad)
                    .map(o -> Recomendacion.newBuilder()
                        .setObra(convertToObraProto(o))
                        .setScore(80)
                        .setRazon("Del mismo estilo: " + obraActual.getEstilo())
                        .build())
                    .collect(Collectors.toList());
            }
            case "MISMO_ARTISTA" -> {
                if (obraActual.getArtista() != null) {
                    sugerencias = obraRepository
                        .findByArtistaId(obraActual.getArtista().getId()).stream()
                        .filter(o -> !o.getId().equals(obraActual.getId()))
                        .limit(cantidad)
                        .map(o -> Recomendacion.newBuilder()
                            .setObra(convertToObraProto(o))
                            .setScore(90)
                            .setRazon("Otra obra de " + obraActual.getArtista().getNombre())
                            .build())
                        .collect(Collectors.toList());
                }
            }
            default -> {
                // Completar visita con obras variadas
                sugerencias = obraRepository.findAll().stream()
                    .filter(o -> !o.getId().equals(obraActual.getId()))
                    .limit(cantidad)
                    .map(o -> Recomendacion.newBuilder()
                        .setObra(convertToObraProto(o))
                        .setScore(70)
                        .setRazon("Para completar tu visita")
                        .build())
                    .collect(Collectors.toList());
            }
        }

        RecomendacionesResponse response = RecomendacionesResponse.newBuilder()
            .addAllRecomendaciones(sugerencias)
            .setTotalDisponibles(sugerencias.size())
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Métodos auxiliares
    private double calcularScorePorPreferencias(ObraEntity obra,
                                                com.unla.museo.usuarios.entity.PreferenciasUsuario prefs) {
        double score = 0;

        if (prefs.getEstilosFavoritos() != null && obra.getEstilo() != null) {
            for (String estilo : prefs.getEstilosFavoritos()) {
                if (obra.getEstilo().equalsIgnoreCase(estilo)) {
                    score += 30;
                }
            }
        }

        if (prefs.getTecnicasFavoritas() != null && obra.getTecnica() != null) {
            for (String tecnica : prefs.getTecnicasFavoritas()) {
                if (obra.getTecnica().equalsIgnoreCase(tecnica)) {
                    score += 25;
                }
            }
        }

        if (prefs.getArtistasFavoritos() != null && obra.getArtista() != null) {
            if (prefs.getArtistasFavoritos().contains(obra.getArtista().getId())) {
                score += 40;
            }
        }

        if (prefs.getUmbralCalificacion() != null) {
            if (obra.getCalificacionPromedio() >= prefs.getUmbralCalificacion()) {
                score += 10;
            }
        }

        // Factor de popularidad
        score += obra.getVisitas() / 100.0;

        return score;
    }

    private String generarRazonPersonal(ObraEntity obra,
                                       com.unla.museo.usuarios.entity.PreferenciasUsuario prefs) {
        StringBuilder razon = new StringBuilder("Te recomendamos esta obra");

        if (prefs.getEstilosFavoritos() != null && obra.getEstilo() != null) {
            for (String estilo : prefs.getEstilosFavoritos()) {
                if (obra.getEstilo().equalsIgnoreCase(estilo)) {
                    razon.append(" porque te interesa el estilo ").append(estilo);
                    return razon.toString();
                }
            }
        }

        if (obra.getCalificacionPromedio() >= 4.0) {
            razon.append(" por su alta calificación de ").append(obra.getCalificacionPromedio());
        } else {
            razon.append(" basada en tus preferencias");
        }

        return razon.toString();
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

        return builder.build();
    }

    private com.unla.museo.catalogo.grpc.Obra convertToObraProto(ObraEntity entity) {
        com.unla.museo.catalogo.grpc.Obra.Builder builder =
            com.unla.museo.catalogo.grpc.Obra.newBuilder()
                .setId(entity.getId())
                .setTitulo(entity.getTitulo())
                .setDescripcion(entity.getDescripcion() != null ? entity.getDescripcion() : "")
                .setTecnica(entity.getTecnica() != null ? entity.getTecnica() : "")
                .setEstilo(entity.getEstilo() != null ? entity.getEstilo() : "")
                .setImagenUrl(entity.getImagenUrl() != null ? entity.getImagenUrl() : "")
                .setAnio(entity.getAnio());

        if (entity.getVisitas() != null) {
            builder.setVisitas(entity.getVisitas());
        }
        if (entity.getCalificacionPromedio() != null) {
            builder.setCalificacionPromedio(entity.getCalificacionPromedio());
        }

        if (entity.getArtista() != null) {
            builder.setArtista(com.unla.museo.catalogo.grpc.Artista.newBuilder()
                .setId(entity.getArtista().getId())
                .setNombre(entity.getArtista().getNombre())
                .build());
        }

        return builder.build();
    }
}
