package com.unla.museo.exploracion.grpc.service;

import com.unla.museo.catalogo.entity.ArtistaEntity;
import com.unla.museo.catalogo.entity.ObraEntity;
import com.unla.museo.catalogo.repository.ArtistaRepository;
import com.unla.museo.catalogo.repository.ObraRepository;
import com.unla.museo.exploracion.grpc.*;
import com.unla.museo.exploracion.grpc.ClusterObras;
import com.unla.museo.exploracion.grpc.ConexionObra;
import com.unla.museo.exploracion.grpc.ConexionesObrasResponse;
import com.unla.museo.exploracion.grpc.CronologiaItem;
import com.unla.museo.exploracion.grpc.DescubrirObrasRequest;
import com.unla.museo.exploracion.grpc.DescubrirObrasResponse;
import com.unla.museo.exploracion.grpc.EspacioArte;
import com.unla.museo.exploracion.grpc.EstadisticasCatalogo;
import com.unla.museo.exploracion.grpc.EstiloCount;
import com.unla.museo.exploracion.grpc.ExploracionServiceGrpc;
import com.unla.museo.exploracion.grpc.ExplorarCronologiaRequest;
import com.unla.museo.exploracion.grpc.ExplorarCronologiaResponse;
import com.unla.museo.exploracion.grpc.ExplorarEspacioRequest;
import com.unla.museo.exploracion.grpc.ExplorarEspacioResponse;
import com.unla.museo.exploracion.grpc.FiltroDinamico;
import com.unla.museo.exploracion.grpc.ObtenerConexionesRequest;
import com.unla.museo.exploracion.grpc.ObtenerEstadisticasRequest;
import com.unla.museo.exploracion.grpc.RangoAnios;
import com.unla.museo.exploracion.grpc.TecnicaCount;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio gRPC para Exploración Interactiva del Catálogo.
 * Proporciona funcionalidades avanzadas de exploración visual y filtrado.
 */
// gRPC atiende fuera de un request web: sin transacción abierta, leer una
// relación LAZY (obra.getArtista()) tira LazyInitializationException.
@GrpcService
@Transactional(readOnly = true)
public class ExploracionCatalogoGrpcService extends ExploracionServiceGrpc.ExploracionServiceImplBase {

    private final ObraRepository obraRepository;
    private final ArtistaRepository artistaRepository;

    public ExploracionCatalogoGrpcService(ObraRepository obraRepository,
                                           ArtistaRepository artistaRepository) {
        this.obraRepository = obraRepository;
        this.artistaRepository = artistaRepository;
    }

    @Override
    public void explorarEspacio(ExplorarEspacioRequest request,
                                StreamObserver<ExplorarEspacioResponse> responseObserver) {
        String modo = request.getModo().toUpperCase();

        EspacioArte.Builder espacioBuilder = EspacioArte.newBuilder();

        List<ObraEntity> todasLasObras = obraRepository.findAll();

        switch (modo) {
            case "ESTILOS" -> {
                // Agrupar por estilo
                Map<String, List<ObraEntity>> obrasPorEstilo = todasLasObras.stream()
                    .filter(o -> o.getEstilo() != null && !o.getEstilo().isEmpty())
                    .collect(Collectors.groupingBy(ObraEntity::getEstilo));

                List<ClusterObras> clusters = obrasPorEstilo.entrySet().stream()
                    .map(entry -> crearCluster(entry.getKey(), "estilo", entry.getValue()))
                    .sorted(Comparator.comparing(ClusterObras::getTotalObras).reversed())
                    .collect(Collectors.toList());

                espacioBuilder.addAllClusters(clusters);

                // Estilos populares
                espacioBuilder.addAllEstilos(obrasPorEstilo.entrySet().stream()
                    .map(e -> {
                        int anios = e.getValue().stream()
                            .mapToInt(ObraEntity::getAnio)
                            .summaryStatistics().getMin();
                        int anioInicio = e.getValue().stream()
                            .mapToInt(ObraEntity::getAnio)
                            .summaryStatistics().getMin();
                        int anioFin = e.getValue().stream()
                            .mapToInt(ObraEntity::getAnio)
                            .summaryStatistics().getMax();

                        return EstiloPopular.newBuilder()
                            .setNombre(e.getKey())
                            .setCantidadObras(e.getValue().size())
                            .setPeriodoInicio(anioInicio)
                            .setPeriodoFin(anioFin)
                            .setDescripcion("Estilo con " + e.getValue().size() + " obras")
                            .build();
                    })
                    .collect(Collectors.toList()));
            }
            case "ARTISTAS" -> {
                List<ArtistaEntity> artistas = artistaRepository.findAll().stream()
                    .sorted(Comparator.comparing(a -> {
                        long obraCount = todasLasObras.stream()
                            .filter(o -> o.getArtista() != null
                                && o.getArtista().getId().equals(a.getId()))
                            .count();
                        return -obraCount;
                    }))
                    .limit(10)
                    .collect(Collectors.toList());

                espacioBuilder.addAllArtistasDestacados(artistas.stream()
                    .map(this::convertToArtistaProto)
                    .collect(Collectors.toList()));
            }
            case "CRONOLOGIA" -> {
                // Por épocas
                espacioBuilder.addAllEpocas(generarEpocas(todasLasObras));
            }
            case "TECNICAS" -> {
                // Agrupar por técnica
                Map<String, List<ObraEntity>> obrasPorTecnica = todasLasObras.stream()
                    .filter(o -> o.getTecnica() != null && !o.getTecnica().isEmpty())
                    .collect(Collectors.groupingBy(ObraEntity::getTecnica));

                List<ClusterObras> clusters = obrasPorTecnica.entrySet().stream()
                    .map(entry -> crearCluster(entry.getKey(), "tecnica", entry.getValue()))
                    .sorted(Comparator.comparing(ClusterObras::getTotalObras).reversed())
                    .collect(Collectors.toList());

                espacioBuilder.addAllClusters(clusters);
            }
            default -> {
                // Vista por defecto: todos los estilos
                espacioBuilder.addAllClusters(
                    todasLasObras.stream()
                        .collect(Collectors.groupingBy(
                            ObraEntity::getEstilo,
                            Collectors.toList()
                        ))
                        .entrySet().stream()
                        .map(e -> crearCluster(e.getKey(), "estilo", e.getValue()))
                        .collect(Collectors.toList())
                );
            }
        }

        ExplorarEspacioResponse response = ExplorarEspacioResponse.newBuilder()
            .setEspacio(espacioBuilder.build())
            .addSugerenciasNavegacion("Explora diferentes estilos")
            .addSugerenciasNavegacion("Descubre artistas destacados")
            .addSugerenciasNavegacion("Viaja en el tiempo cronológico")
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void descubrirObras(DescubrirObrasRequest request,
                               StreamObserver<DescubrirObrasResponse> responseObserver) {
        List<ObraEntity> obras = obraRepository.findAll();

        // Aplicar filtros dinámicos
        for (FiltroDinamico filtro : request.getFiltrosList()) {
            obras = aplicarFiltro(obras, filtro);
        }

        // Ordenar
        final List<ObraEntity> finalObras = obras;
        Comparator<ObraEntity> comparador = switch (request.getOrdenarPor().toLowerCase()) {
            case "titulo" -> Comparator.comparing(ObraEntity::getTitulo);
            case "anio" -> Comparator.comparing(ObraEntity::getAnio);
            case "visitas" -> Comparator.comparing(ObraEntity::getVisitas);
            case "calificacion" -> Comparator.comparing(ObraEntity::getCalificacionPromedio);
            default -> Comparator.comparing(ObraEntity::getId);
        };

        if (request.getOrdenDescendente()) {
            comparador = comparador.reversed();
        }

        obras = finalObras.stream()
            .sorted(comparador)
            .skip(request.getPagina() * request.getTamano())
            .limit(request.getTamano() > 0 ? request.getTamano() : 10)
            .collect(Collectors.toList());

        // Sugerir filtros basados en resultados
        List<String> sugerenciasFiltros = generarSugerenciasFiltros(finalObras);

        DescubrirObrasResponse response = DescubrirObrasResponse.newBuilder()
            .addAllObras(obras.stream()
                .map(this::convertToObraProto)
                .collect(Collectors.toList()))
            .setTotal(finalObras.size())
            .addAllFiltrosAplicados(request.getFiltrosList())
            .addAllSugerenciasFiltros(sugerenciasFiltros)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void explorarCronologia(ExplorarCronologiaRequest request,
                                   StreamObserver<ExplorarCronologiaResponse> responseObserver) {
        int anioInicio = request.getAnioInicio() > 0 ? request.getAnioInicio() : 1500;
        int anioFin = request.getAnioFin() > 0 ? request.getAnioFin() : 2000;

        List<ObraEntity> obrasEnRango = obraRepository.findAll().stream()
            .filter(o -> o.getAnio() >= anioInicio && o.getAnio() <= anioFin)
            .collect(Collectors.toList());

        if (!request.getFiltroEstilo().isEmpty()) {
            obrasEnRango = obrasEnRango.stream()
                .filter(o -> o.getEstilo() != null
                    && o.getEstilo().equalsIgnoreCase(request.getFiltroEstilo()))
                .collect(Collectors.toList());
        }

        // Agrupar por periodos de 25 años
        Map<String, List<ObraEntity>> obrasPorPeriodo = obrasEnRango.stream()
            .collect(Collectors.groupingBy(
                o -> {
                    int periodo = (o.getAnio() / 25) * 25;
                    return periodo + "-" + (periodo + 24);
                },
                Collectors.toList()
            ));

        List<CronologiaItem> items = obrasPorPeriodo.entrySet().stream()
            .sorted(Comparator.comparing(e -> Integer.parseInt(e.getKey().split("-")[0])))
            .map(e -> {
                List<ObraEntity> obrasPeriodo = e.getValue();
                int inicio = Integer.parseInt(e.getKey().split("-")[0]);
                int fin = Integer.parseInt(e.getKey().split("-")[1]);

                return CronologiaItem.newBuilder()
                    .setPeriodo("Años " + inicio + " - " + fin)
                    .setAnioInicio(inicio)
                    .setAnioFin(fin)
                    .addAllObrasDestacadas(obrasPeriodo.stream()
                        .limit(5)
                        .map(this::convertToObraProto)
                        .collect(Collectors.toList()))
                    .setCantidadObras(obrasPeriodo.size())
                    .build();
            })
            .collect(Collectors.toList());

        // Detectar línea temática
        String lineaTematica = detectarLineaTematica(obrasEnRango);

        ExplorarCronologiaResponse response = ExplorarCronologiaResponse.newBuilder()
            .addAllItems(items)
            .setLineaTematicaDetectada(lineaTematica)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void obtenerConexionesObras(ObtenerConexionesRequest request,
                                       StreamObserver<ConexionesObrasResponse> responseObserver) {
        Optional<ObraEntity> obraOpt = obraRepository.findById(request.getObraId());

        if (obraOpt.isEmpty()) {
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription("Obra no encontrada: " + request.getObraId())
                    .asRuntimeException()
            );
            return;
        }

        ObraEntity obraOrigen = obraOpt.get();
        List<ConexionObra> conexiones = new ArrayList<>();
        Set<Long> obrasRelacionadasIds = new HashSet<>();
        int profundidad = Math.min(request.getProfundidad(), 3);

        // Buscar conexiones
        // 1. Mismo estilo
        List<ObraEntity> mismoEstilo = obraRepository
            .findByEstiloContainingIgnoreCase(obraOrigen.getEstilo());
        for (ObraEntity obra : mismoEstilo) {
            if (!obra.getId().equals(obraOrigen.getId())) {
                conexiones.add(ConexionObra.newBuilder()
                    .setObraOrigenId(obraOrigen.getId())
                    .setObraDestinoId(obra.getId())
                    .setTipoConexion("MISMO_ESTILO")
                    .setFuerza(0.8)
                    .setDescripcion("Comparten el estilo " + obraOrigen.getEstilo())
                    .build());
                obrasRelacionadasIds.add(obra.getId());
            }
        }

        // 2. Mismo artista
        if (obraOrigen.getArtista() != null) {
            List<ObraEntity> mismoArtista = obraRepository
                .findByArtistaId(obraOrigen.getArtista().getId());
            for (ObraEntity obra : mismoArtista) {
                if (!obra.getId().equals(obraOrigen.getId())) {
                    conexiones.add(ConexionObra.newBuilder()
                        .setObraOrigenId(obraOrigen.getId())
                        .setObraDestinoId(obra.getId())
                        .setTipoConexion("MISMO_ARTISTA")
                        .setFuerza(1.0)
                        .setDescripcion("Del mismo artista: " + obraOrigen.getArtista().getNombre())
                        .build());
                    obrasRelacionadasIds.add(obra.getId());
                }
            }
        }

        // 3. Contemporáneas (dentro de 25 años)
        List<ObraEntity> contemporaneas = obraRepository.findAll().stream()
            .filter(o -> !o.getId().equals(obraOrigen.getId()))
            .filter(o -> Math.abs(o.getAnio() - obraOrigen.getAnio()) <= 25)
            .collect(Collectors.toList());
        for (ObraEntity obra : contemporaneas) {
            if (!obrasRelacionadasIds.contains(obra.getId())) {
                conexiones.add(ConexionObra.newBuilder()
                    .setObraOrigenId(obraOrigen.getId())
                    .setObraDestinoId(obra.getId())
                    .setTipoConexion("CONTEMPORANEAS")
                    .setFuerza(0.6)
                    .setDescripcion("Obras contemporáneas")
                    .build());
                obrasRelacionadasIds.add(obra.getId());
            }
        }

        // Obtener obras relacionadas
        List<com.unla.museo.catalogo.grpc.Obra> obrasRelacionadas = obrasRelacionadasIds.stream()
            .map(obraRepository::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(this::convertToObraProto)
            .collect(Collectors.toList());

        ConexionesObrasResponse response = ConexionesObrasResponse.newBuilder()
            .addAllConexiones(conexiones)
            .addAllObrasRelacionadas(obrasRelacionadas)
            .setObrasTotalesConectadas(obrasRelacionadas.size())
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void obtenerEstadisticas(ObtenerEstadisticasRequest request,
                                    StreamObserver<EstadisticasCatalogo> responseObserver) {
        List<ObraEntity> todasLasObras = obraRepository.findAll();
        List<ObraEntity> obras = todasLasObras;

        if (!request.getFiltroEstilo().isEmpty()) {
            obras = obras.stream()
                .filter(o -> o.getEstilo() != null
                    && o.getEstilo().equalsIgnoreCase(request.getFiltroEstilo()))
                .collect(Collectors.toList());
        }

        // Estilos
        Map<String, Long> estilos = obras.stream()
            .filter(o -> o.getEstilo() != null)
            .collect(Collectors.groupingBy(ObraEntity::getEstilo, Collectors.counting()));

        List<ObraEntity> finalObras = obras;
        List<EstiloCount> estilosCount = estilos.entrySet().stream()
            .map(e -> EstiloCount.newBuilder()
                .setEstilo(e.getKey())
                .setCantidad(e.getValue().intValue())
                .setPorcentaje((e.getValue() * 100.0) / finalObras.size())
                .build())
            .sorted(Comparator.comparing(EstiloCount::getCantidad).reversed())
            .collect(Collectors.toList());

        // Técnicas
        Map<String, Long> tecnicas = obras.stream()
            .filter(o -> o.getTecnica() != null)
            .collect(Collectors.groupingBy(ObraEntity::getTecnica, Collectors.counting()));

        List<TecnicaCount> tecnicasCount = tecnicas.entrySet().stream()
            .map(e -> TecnicaCount.newBuilder()
                .setTecnica(e.getKey())
                .setCantidad(e.getValue().intValue())
                .build())
            .sorted(Comparator.comparing(TecnicaCount::getCantidad).reversed())
            .collect(Collectors.toList());

        // Rangos de años
        Map<String, Long> periodos = obras.stream()
            .collect(Collectors.groupingBy(
                o -> String.valueOf((o.getAnio() / 50) * 50),
                Collectors.counting()
            ));

        List<RangoAnios> rangos = periodos.entrySet().stream()
            .map(e -> RangoAnios.newBuilder()
                .setRango(e.getKey() + " - " + (Integer.parseInt(e.getKey()) + 49))
                .setCantidad(e.getValue().intValue())
                .build())
            .sorted(Comparator.comparing(r -> Integer.parseInt(r.getRango().split(" - ")[0])))
            .collect(Collectors.toList());

        double calificacionPromedio = obras.stream()
            .mapToDouble(ObraEntity::getCalificacionPromedio)
            .average()
            .orElse(0.0);

        EstadisticasCatalogo estadisticas = EstadisticasCatalogo.newBuilder()
            .setTotalObras(obras.size())
            .setTotalArtistas((int) obras.stream()
                .filter(o -> o.getArtista() != null)
                .map(o -> o.getArtista().getId())
                .distinct()
                .count())
            .addAllEstilos(estilosCount)
            .addAllTecnicas(tecnicasCount)
            .addAllPeriodos(rangos)
            .setCalificacionPromedioGlobal(calificacionPromedio)
            .build();

        responseObserver.onNext(estadisticas);
        responseObserver.onCompleted();
    }

    // Métodos auxiliares
    private ClusterObras crearCluster(String nombre, String categoria, List<ObraEntity> obras) {
        return ClusterObras.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setNombre(nombre)
            .setCategoria(categoria)
            .addAllObrasMuestra(obras.stream()
                .limit(3)
                .map(this::convertToObraProto)
                .collect(Collectors.toList()))
            .setTotalObras(obras.size())
            .setX(Math.random() * 100)
            .setY(Math.random() * 100)
            .setColor(generarColorParaCategoria(nombre))
            .build();
    }

    private String generarColorParaCategoria(String nombre) {
        int hash = nombre.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;
        return String.format("#%02X%02X%02X", r % 200 + 55, g % 200 + 55, b % 200 + 55);
    }

    private List<EpocaArte> generarEpocas(List<ObraEntity> obras) {
        return obras.stream()
            .collect(Collectors.groupingBy(
                o -> String.valueOf((o.getAnio() / 100) * 100),
                Collectors.toList()
            ))
            .entrySet().stream()
            .sorted(Comparator.comparing(e -> Integer.parseInt(e.getKey())))
            .map(e -> {
                int inicio = Integer.parseInt(e.getKey());
                int fin = inicio + 99;
                List<String> estilos = e.getValue().stream()
                    .map(ObraEntity::getEstilo)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

                return EpocaArte.newBuilder()
                    .setNombre(obtenerNombreEpoca(inicio))
                    .setRangoAnios(inicio + " - " + fin)
                    .setCantidadObras(e.getValue().size())
                    .addAllEstilosPredominantes(estilos)
                    .build();
            })
            .collect(Collectors.toList());
    }

    private String obtenerNombreEpoca(int siglo) {
        return switch (siglo) {
            case 1400 -> "Renacimiento Temprano";
            case 1500 -> "Alta Renacimiento";
            case 1600 -> "Barroco";
            case 1700 -> "Rococó y Neoclasicismo";
            case 1800 -> "Romanticismo y Realismo";
            case 1900 -> "Modernismo y Vanguardias";
            case 2000 -> "Arte Contemporáneo";
            default -> "Época " + siglo + "s";
        };
    }

    private List<ObraEntity> aplicarFiltro(List<ObraEntity> obras, FiltroDinamico filtro) {
        String campo = filtro.getCampo().toLowerCase();
        String operador = filtro.getOperador().toLowerCase();
        String valor = filtro.getValor();

        return obras.stream().filter(o -> {
            String valorCampo = switch (campo) {
                case "titulo" -> o.getTitulo();
                case "estilo" -> o.getEstilo();
                case "tecnica" -> o.getTecnica();
                case "anio" -> String.valueOf(o.getAnio());
                case "artista" -> o.getArtista() != null ? o.getArtista().getNombre() : "";
                case "calificacion" -> String.valueOf(o.getCalificacionPromedio());
                default -> "";
            };

            return switch (operador) {
                case "eq", "==" -> valorCampo.equalsIgnoreCase(valor);
                case "lk", "like" -> valorCampo.toLowerCase().contains(valor.toLowerCase());
                case "gt", ">" -> {
                    try {
                        yield Double.parseDouble(valorCampo) > Double.parseDouble(valor);
                    } catch (NumberFormatException e) {
                        yield false;
                    }
                }
                case "lt", "<" -> {
                    try {
                        yield Double.parseDouble(valorCampo) < Double.parseDouble(valor);
                    } catch (NumberFormatException e) {
                        yield false;
                    }
                }
                default -> true;
            };
        }).collect(Collectors.toList());
    }

    private List<String> generarSugerenciasFiltros(List<ObraEntity> obras) {
        List<String> sugerencias = new ArrayList<>();

        // SUgerir estilos populares
        Map<String, Long> estilos = obras.stream()
            .filter(o -> o.getEstilo() != null)
            .collect(Collectors.groupingBy(ObraEntity::getEstilo, Collectors.counting()));

        estilos.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(3)
            .forEach(e -> sugerencias.add("Estilo: " + e.getKey() + " (" + e.getValue() + ")"));

        // Sugerir técnicas
        Map<String, Long> tecnicas = obras.stream()
            .filter(o -> o.getTecnica() != null)
            .collect(Collectors.groupingBy(ObraEntity::getTecnica, Collectors.counting()));

        tecnicas.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(2)
            .forEach(e -> sugerencias.add("Técnica: " + e.getKey()));

        return sugerencias;
    }

    private String detectarLineaTematica(List<ObraEntity> obras) {
        // Detectar tema común basado en estilos predominantes
        Map<String, Long> estilos = obras.stream()
            .filter(o -> o.getEstilo() != null)
            .collect(Collectors.groupingBy(ObraEntity::getEstilo, Collectors.counting()));

        if (estilos.isEmpty()) return "Colección variada";

        String estiloMayor = estilos.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Variado");

        return "Evolución del " + estiloMayor;
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

    private com.unla.museo.catalogo.grpc.Artista convertToArtistaProto(ArtistaEntity entity) {
        return com.unla.museo.catalogo.grpc.Artista.newBuilder()
            .setId(entity.getId())
            .setNombre(entity.getNombre())
            .setNacionalidad(entity.getNacionalidad() != null ? entity.getNacionalidad() : "")
            .build();
    }
}
