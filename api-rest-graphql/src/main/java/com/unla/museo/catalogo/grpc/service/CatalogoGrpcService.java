package com.unla.museo.catalogo.grpc.service;

import com.unla.museo.catalogo.entity.ArtistaEntity;
import com.unla.museo.catalogo.entity.ComentarioEntity;
import com.unla.museo.catalogo.entity.ObraEntity;
import com.unla.museo.catalogo.grpc.*;
import com.unla.museo.catalogo.grpc.CatalogoServiceGrpc;
import com.unla.museo.catalogo.repository.ArtistaRepository;
import com.unla.museo.catalogo.repository.ComentarioRepository;
import com.unla.museo.catalogo.repository.ObraRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio gRPC para el Catálogo de Obras y Artistas.
 * Expone operaciones CRUD y búsquedas sobre el catálogo del museo.
 */
@GrpcService
public class CatalogoGrpcService extends CatalogoServiceGrpc.CatalogoServiceImplBase {

    private final ObraRepository obraRepository;
    private final ArtistaRepository artistaRepository;
    private final ComentarioRepository comentarioRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public CatalogoGrpcService(ObraRepository obraRepository,
                                ArtistaRepository artistaRepository,
                                ComentarioRepository comentarioRepository) {
        this.obraRepository = obraRepository;
        this.artistaRepository = artistaRepository;
        this.comentarioRepository = comentarioRepository;
    }

    @Override
    public void listarObras(ListarObrasRequest request,
                            StreamObserver<ListarObrasResponse> responseObserver) {
        int pagina = request.getPagina() > 0 ? request.getPagina() : 0;
        int tamano = request.getTamano() > 0 ? request.getTamano() : 10;
        String ordenarPor = request.getOrdenarPor().isEmpty() ? "id" : request.getOrdenarPor();
        String orden = request.getOrden().isEmpty() ? "ASC" : request.getOrden();

        List<ObraEntity> todasLasObras = obraRepository.findAll();

        // Aplicar ordenamiento
        Comparator<ObraEntity> comparador = switch (ordenarPor.toLowerCase()) {
            case "titulo" -> Comparator.comparing(ObraEntity::getTitulo);
            case "anio" -> Comparator.comparing(ObraEntity::getAnio);
            case "calificacion" -> Comparator.comparing(ObraEntity::getCalificacionPromedio);
            case "visitas" -> Comparator.comparing(ObraEntity::getVisitas);
            default -> Comparator.comparing(ObraEntity::getId);
        };

        if (orden.equalsIgnoreCase("DESC")) {
            comparador = comparador.reversed();
        }

        List<ObraEntity> obrasOrdenadas = todasLasObras.stream()
            .sorted(comparador)
            .collect(Collectors.toList());

        int total = obrasOrdenadas.size();
        int totalPaginas = (int) Math.ceil((double) total / tamano);

        List<ObraEntity> obrasPaginadas = obrasOrdenadas.stream()
            .skip((long) pagina * tamano)
            .limit(tamano)
            .collect(Collectors.toList());

        ListarObrasResponse response = ListarObrasResponse.newBuilder()
            .addAllObras(obrasPaginadas.stream()
                .map(this::convertToObraProto)
                .collect(Collectors.toList()))
            .setTotal(total)
            .setPagina(pagina)
            .setTotalPaginas(totalPaginas)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void obtenerObra(ObtenerObraRequest request,
                            StreamObserver<Obra> responseObserver) {
        Optional<ObraEntity> obraOpt = obraRepository.findById(request.getId());

        if (obraOpt.isPresent()) {
            Obra obra = convertToObraProto(obraOpt.get());
            responseObserver.onNext(obra);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription("Obra no encontrada: " + request.getId())
                    .asRuntimeException()
            );
        }
    }

    @Override
    public void buscarObras(BuscarObrasRequest request,
                            StreamObserver<BuscarObrasResponse> responseObserver) {
        int pagina = request.getPagina() > 0 ? request.getPagina() : 0;
        int tamano = request.getTamano() > 0 ? request.getTamano() : 10;

        List<ObraEntity> obras = obraRepository.findAll();

        // Filtrar por consulta general
        if (!request.getConsulta().isEmpty()) {
            String query = request.getConsulta().toLowerCase();
            obras = obras.stream()
                .filter(o -> o.getTitulo().toLowerCase().contains(query) ||
                            (o.getDescripcion() != null && o.getDescripcion().toLowerCase().contains(query)) ||
                            (o.getTecnica() != null && o.getTecnica().toLowerCase().contains(query)) ||
                            (o.getEstilo() != null && o.getEstilo().toLowerCase().contains(query)) ||
                            (o.getArtista() != null && o.getArtista().getNombre().toLowerCase().contains(query)))
                .collect(Collectors.toList());
        }

        // Filtrar por técnica
        if (!request.getTecnica().isEmpty()) {
            obras = obras.stream()
                .filter(o -> o.getTecnica() != null &&
                            o.getTecnica().equalsIgnoreCase(request.getTecnica()))
                .collect(Collectors.toList());
        }

        // Filtrar por estilo
        if (!request.getEstilo().isEmpty()) {
            obras = obras.stream()
                .filter(o -> o.getEstilo() != null &&
                            o.getEstilo().equalsIgnoreCase(request.getEstilo()))
                .collect(Collectors.toList());
        }

        // Filtrar por artista
        if (request.getArtistaId() > 0) {
            obras = obras.stream()
                .filter(o -> o.getArtista() != null &&
                            o.getArtista().getId().equals(request.getArtistaId()))
                .collect(Collectors.toList());
        }

        int total = obras.size();

        List<Obra> obrasPaginadas = obras.stream()
            .skip((long) pagina * tamano)
            .limit(tamano)
            .map(this::convertToObraProto)
            .collect(Collectors.toList());

        BuscarObrasResponse response = BuscarObrasResponse.newBuilder()
            .addAllObras(obrasPaginadas)
            .setTotal(total)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listarArtistas(ListarArtistasRequest request,
                               StreamObserver<ListarArtistasResponse> responseObserver) {
        int pagina = request.getPagina() > 0 ? request.getPagina() : 0;
        int tamano = request.getTamano() > 0 ? request.getTamano() : 10;

        List<ArtistaEntity> todosLosArtistas = artistaRepository.findAll();
        int total = todosLosArtistas.size();

        List<Artista> artistasPaginados = todosLosArtistas.stream()
            .skip((long) pagina * tamano)
            .limit(tamano)
            .map(this::convertToArtistaProto)
            .collect(Collectors.toList());

        ListarArtistasResponse response = ListarArtistasResponse.newBuilder()
            .addAllArtistas(artistasPaginados)
            .setTotal(total)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void obtenerArtista(ObtenerArtistaRequest request,
                                 StreamObserver<Artista> responseObserver) {
        Optional<ArtistaEntity> artistaOpt = artistaRepository.findById(request.getId());

        if (artistaOpt.isPresent()) {
            Artista artista = convertToArtistaProto(artistaOpt.get());
            responseObserver.onNext(artista);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription("Artista no encontrado: " + request.getId())
                    .asRuntimeException()
            );
        }
    }

    @Override
    public void listarComentarios(ListarComentariosRequest request,
                                  StreamObserver<ListarComentariosResponse> responseObserver) {
        int pagina = request.getPagina() > 0 ? request.getPagina() : 0;
        int tamano = request.getTamano() > 0 ? request.getTamano() : 10;

        List<ComentarioEntity> comentarios = comentarioRepository
            .findByObraId(request.getObraId());

        int total = comentarios.size();

        List<Comentario> comentariosPaginados = comentarios.stream()
            .skip((long) pagina * tamano)
            .limit(tamano)
            .map(this::convertToComentarioProto)
            .collect(Collectors.toList());

        ListarComentariosResponse response = ListarComentariosResponse.newBuilder()
            .addAllComentarios(comentariosPaginados)
            .setTotal(total)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void crearComentario(CrearComentarioRequest request,
                                StreamObserver<Comentario> responseObserver) {
        ComentarioEntity comentario = new ComentarioEntity();
        comentario.setTexto(request.getContenido());
        comentario.setFecha(LocalDateTime.now());

        ObraEntity obra = obraRepository.findById(request.getObraId()).orElse(null);
        if (obra != null) {
            comentario.setObra(obra);
        }

        // Guardar el comentario
        ComentarioEntity guardado = comentarioRepository.save(comentario);

        Comentario response = convertToComentarioProto(guardado);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Métodos auxiliares de conversión
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

    private Artista convertToArtistaProto(ArtistaEntity entity) {
        return Artista.newBuilder()
            .setId(entity.getId())
            .setNombre(entity.getNombre())
            .setNacionalidad(entity.getNacionalidad() != null ? entity.getNacionalidad() : "")
            .setBiografia(entity.getBiografia() != null ? entity.getBiografia() : "")
            .setImagenUrl(entity.getImagenUrl() != null ? entity.getImagenUrl() : "")
            .build();
    }

    private Comentario convertToComentarioProto(ComentarioEntity entity) {
        Comentario.Builder builder = Comentario.newBuilder()
            .setId(entity.getId())
            .setContenido(entity.getTexto())
            .setCalificacion(0); // La entidad no tiene calificación

        if (entity.getObra() != null) {
            builder.setObraId(entity.getObra().getId());
        }
        if (entity.getUsuario() != null) {
            builder.setUsuarioId(entity.getUsuario().getId());
        }
        if (entity.getFecha() != null) {
            builder.setFecha(entity.getFecha().format(DATE_FORMATTER));
        }

        return builder.build();
    }
}
