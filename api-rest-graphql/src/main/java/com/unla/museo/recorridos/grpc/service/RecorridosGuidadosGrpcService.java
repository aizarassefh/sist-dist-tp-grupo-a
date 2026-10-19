package com.unla.museo.recorridos.grpc.service;

import com.unla.museo.catalogo.grpc.Artista;
import com.unla.museo.catalogo.grpc.Obra;
import com.unla.museo.catalogo.entity.ObraEntity;
import com.unla.museo.catalogo.repository.ObraRepository;
import com.unla.museo.recorridos.grpc.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio gRPC para Recorridos Guiados Personalizados.
 * Genera recorridos temáticos optimizados basados en diferentes criterios.
 */
@GrpcService
public class RecorridosGuidadosGrpcService extends RecorridosServiceGrpc.RecorridosServiceImplBase {

    private final ObraRepository obraRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    // Tiempo promedio de visita por obra en minutos
    private static final int TIEMPO_VISITA_POR_OBRA = 5;

    public RecorridosGuidadosGrpcService(ObraRepository obraRepository) {
        this.obraRepository = obraRepository;
    }

    @Override
    public void generarRecorrido(GenerarRecorridoRequest request,
                                 StreamObserver<Recorrido> responseObserver) {
        CriteriosRecorrido criterios = request.getCriterios();
        List<ObraEntity> obrasSeleccionadas = new ArrayList<>();
        String temaRecorrido = "Recorrido personalizado";

        try {
            switch (criterios.getTipo()) {
                case POR_ESTILO -> {
                    temaRecorrido = "Estilo: " + criterios.getEstilo();
                    obrasSeleccionadas = obraRepository
                        .findByEstiloContainingIgnoreCase(criterios.getEstilo())
                        .stream()
                        .limit(criterios.getCantidadMaximaObras() > 0
                            ? criterios.getCantidadMaximaObras()
                            : 10)
                        .collect(Collectors.toList());
                }
                case POR_ARTISTA -> {
                    Optional<ObraEntity> obraEjemplo = obraRepository
                        .findById(criterios.getArtistaId());
                    if (obraEjemplo.isPresent()) {
                        String nombreArtista = obraEjemplo.get().getArtista() != null
                            ? obraEjemplo.get().getArtista().getNombre()
                            : "Artista";
                        temaRecorrido = "Obras de " + nombreArtista;
                    }
                    obrasSeleccionadas = obraRepository
                        .findByArtistaId(criterios.getArtistaId())
                        .stream()
                        .limit(criterios.getCantidadMaximaObras() > 0
                            ? criterios.getCantidadMaximaObras()
                            : 10)
                        .collect(Collectors.toList());
                }
                case POR_EPOCA -> {
                    temaRecorrido = "Época: " + criterios.getFechaInicio() + " - " + criterios.getFechaFin();
                    // Implementar búsqueda por rango de fechas
                    obrasSeleccionadas = obraRepository.findAll()
                        .stream()
                        .filter(o -> isInRange(o.getAnio(), criterios.getFechaInicio(), criterios.getFechaFin()))
                        .limit(criterios.getCantidadMaximaObras() > 0
                            ? criterios.getCantidadMaximaObras()
                            : 10)
                        .collect(Collectors.toList());
                }
                case POR_DURACION -> {
                    int duracionMinutos = criterios.getDuracionMinutos();
                    int maxObras = duracionMinutos / TIEMPO_VISITA_POR_OBRA;
                    temaRecorrido = "Recorrido de " + duracionMinutos + " minutos";
                    obrasSeleccionadas = obraRepository.findAll()
                        .stream()
                        .sorted(Comparator.comparing(ObraEntity::getCalificacionPromedio).reversed())
                        .limit(maxObras)
                        .collect(Collectors.toList());
                }
                case PERSONALIZADO -> {
                    temaRecorrido = "Recorrido personalizado para ti";
                    // Consultar preferencias del usuario y generar recorrido
                    obrasSeleccionadas = generarRecorridoPersonalizado(criterios.getUsuarioId());
                }
                default -> obrasSeleccionadas = obraRepository.findAll()
                    .stream()
                    .limit(10)
                    .collect(Collectors.toList());
            }

            // Construir recorrido
            Recorrido.Builder recorridoBuilder = Recorrido.newBuilder()
                .setId(System.currentTimeMillis())
                .setNombre(request.getNombrePersonalizado().isEmpty()
                    ? temaRecorrido
                    : request.getNombrePersonalizado())
                .setDescripcion("Recorrido generado automáticamente: " + temaRecorrido)
                .setUsuarioId(request.getUsuarioId())
                .setDuracionEstimadaMinutos(obrasSeleccionadas.size() * TIEMPO_VISITA_POR_OBRA)
                .setDistanciaTotalMetros(calcularDistanciaTotal(obrasSeleccionadas.size()))
                .setTema(temaRecorrido)
                .setFechaCreacion(LocalDateTime.now().format(DATE_FORMATTER))
                .setCompartible(true);

            // Agregar paradas
            int orden = 1;
            for (ObraEntity obra : obrasSeleccionadas) {
                ParadaRecorrido parada = ParadaRecorrido.newBuilder()
                    .setOrden(orden++)
                    .setObra(convertToObraProto(obra))
                    .setTiempoSugeridoMinutos(TIEMPO_VISITA_POR_OBRA)
                    .setNotas("Visita obligada para apreciar " + obra.getTecnica())
                    .build();
                recorridoBuilder.addParadas(parada);
            }

            Recorrido recorrido = recorridoBuilder.build();

            // Guardar si es necesario
            if (request.getGuardarAutomaticamente()) {
                guardarRecorrido(recorrido);
            }

            responseObserver.onNext(recorrido);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(
                io.grpc.Status.INTERNAL
                    .withDescription("Error generando recorrido: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    @Override
    public void guardarRecorrido(GuardarRecorridoRequest request,
                                 StreamObserver<Recorrido> responseObserver) {
        Recorrido recorrido = Recorrido.newBuilder()
            .setId(System.currentTimeMillis())
            .setNombre(request.getNombre())
            .setDescripcion(request.getDescripcion())
            .setUsuarioId(request.getUsuarioId())
            .addAllParadas(request.getParadasList())
            .setTema(request.getTema())
            .setFechaCreacion(LocalDateTime.now().format(DATE_FORMATTER))
            .setCompartible(request.getCompartible())
            .build();

        guardarRecorrido(recorrido);

        responseObserver.onNext(recorrido);
        responseObserver.onCompleted();
    }

    @Override
    public void obtenerRecorrido(ObtenerRecorridoRequest request,
                                StreamObserver<Recorrido> responseObserver) {
        // Aquí se consultaría el recorrido guardado
        // Por ahora retornamos un recorrido de ejemplo
        Recorrido recorrido = crearRecorridoEjemplo(request.getRecorridoId());
        responseObserver.onNext(recorrido);
        responseObserver.onCompleted();
    }

    @Override
    public void listarRecorridosUsuario(ListarRecorridosRequest request,
                                       StreamObserver<ListarRecorridosResponse> responseObserver) {
        // Aquí se consultarían los recorridos del usuario
        // Por ahora retornamos recorridos de ejemplo
        List<Recorrido> recorridos = List.of(
            crearRecorridoEjemplo(1L),
            crearRecorridoEjemplo(2L),
            crearRecorridoEjemplo(3L)
        );

        ListarRecorridosResponse response = ListarRecorridosResponse.newBuilder()
            .addAllRecorridos(recorridos)
            .setTotal(recorridos.size())
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void eliminarRecorrido(EliminarRecorridoRequest request,
                                 StreamObserver<EliminarRecorridoResponse> responseObserver) {
        // Aquí se eliminaría el recorrido
        EliminarRecorridoResponse response = EliminarRecorridoResponse.newBuilder()
            .setExito(true)
            .setMensaje("Recorrido eliminado correctamente")
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Métodos auxiliares
    private List<ObraEntity> generarRecorridoPersonalizado(long usuarioId) {
        // Por ahora retorna las obras mejor calificadas
        return obraRepository.findAll()
            .stream()
            .sorted(Comparator.comparing(ObraEntity::getCalificacionPromedio).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }

    private void guardarRecorrido(Recorrido recorrido) {
        // Persistir en base de datos
        // Implementación depende del repositorio de recorridos
    }

    private double calcularDistanciaTotal(int cantidadObras) {
        // Simulación: cada obra está aproximadamente a 50 metros de la siguiente
        return (cantidadObras - 1) * 50.0;
    }

    private boolean isInRange(int anio, String fechaInicio, String fechaFin) {
        try {
            int anioInicio = fechaInicio.length() >= 4 ? Integer.parseInt(fechaInicio.substring(0, 4)) : 1800;
            int anioFin = fechaFin.length() >= 4 ? Integer.parseInt(fechaFin.substring(0, 4)) : 2024;
            return anio >= anioInicio && anio <= anioFin;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private Obra convertToObraProto(ObraEntity entity) {
        Obra.Builder builder = Obra.newBuilder()
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
            builder.setArtista(Artista.newBuilder()
                .setId(entity.getArtista().getId())
                .setNombre(entity.getArtista().getNombre())
                .setNacionalidad(entity.getArtista().getNacionalidad() != null
                    ? entity.getArtista().getNacionalidad() : "")
                .build());
        }

        return builder.build();
    }

    private Recorrido crearRecorridoEjemplo(long id) {
        return Recorrido.newBuilder()
            .setId(id)
            .setNombre("Recorrido de ejemplo " + id)
            .setDescripcion("Este es un recorrido de ejemplo")
            .setUsuarioId(1L)
            .setDuracionEstimadaMinutos(30)
            .setDistanciaTotalMetros(200.0)
            .setTema("Recorrido temático")
            .setFechaCreacion(LocalDateTime.now().format(DATE_FORMATTER))
            .setCompartible(true)
            .build();
    }
}
