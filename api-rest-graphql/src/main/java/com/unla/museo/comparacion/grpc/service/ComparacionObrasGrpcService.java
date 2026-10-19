package com.unla.museo.comparacion.grpc.service;

import com.unla.museo.catalogo.entity.ObraEntity;
import com.unla.museo.catalogo.repository.ObraRepository;
import com.unla.museo.comparacion.grpc.*;
import com.unla.museo.comparacion.grpc.ComparacionServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio gRPC para Comparación de Obras.
 * Permite comparar obras lado a lado y encontrar similitudes/diferencias.
 */
@GrpcService
public class ComparacionObrasGrpcService extends ComparacionServiceGrpc.ComparacionServiceImplBase {

    private final ObraRepository obraRepository;

    public ComparacionObrasGrpcService(ObraRepository obraRepository) {
        this.obraRepository = obraRepository;
    }

    @Override
    public void compararObras(CompararObrasRequest request,
                              StreamObserver<ComparacionObras> responseObserver) {
        Optional<ObraEntity> obraPrincipalOpt = obraRepository.findById(request.getObraPrincipalId());
        Optional<ObraEntity> obraSecundariaOpt = obraRepository.findById(request.getObraSecundariaId());

        if (obraPrincipalOpt.isEmpty() || obraSecundariaOpt.isEmpty()) {
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription("Una o ambas obras no fueron encontradas")
                    .asRuntimeException()
            );
            return;
        }

        ObraEntity principal = obraPrincipalOpt.get();
        ObraEntity secundaria = obraSecundariaOpt.get();

        // Calcular dimensiones de comparación
        DimensionComparacion tecnica = compararTecnica(principal, secundaria);
        DimensionComparacion estilo = compararEstilo(principal, secundaria);
        DimensionComparacion epoca = compararEpoca(principal, secundaria);
        DimensionComparacion artista = compararArtista(principal, secundaria);
        DimensionComparacion tematica = compararTematica(principal, secundaria);

        // Calcular índice general de similitud
        double indiceSimilitud = calcularIndiceSimilitud(
            tecnica.getSimilitud(),
            estilo.getSimilitud(),
            epoca.getSimilitud(),
            artista.getSimilitud(),
            tematica.getSimilitud()
        );

        // Generar análisis y punto
        List<String> similitudes = new ArrayList<>();
        List<String> diferencias = new ArrayList<>();

        if (tecnica.getSimilitud() > 70) {
            similitudes.add("Ambas obras emplean técnicas similares: " + tecnica.getValorObraPrincipal());
        } else {
            diferencias.add("Diferencia en técnica: " + principal.getTecnica() + " vs " + secundaria.getTecnica());
        }

        if (estilo.getSimilitud() > 70) {
            similitudes.add("Comparten el mismo estilo artístico: " + estilo.getValorObraPrincipal());
        } else {
            diferencias.add("Diferencia de estilo: " + principal.getEstilo() + " vs " + secundaria.getEstilo());
        }

        if (artista.getSimilitud() > 90) {
            similitudes.add("Ambas obras son del mismo artista");
        }

        if (epoca.getSimilitud() > 70) {
            similitudes.add("Pertenecen a la misma época");
        }

        String analisis = generarAnalisis(principal, secundaria, indiceSimilitud);

        ComparacionObras comparacion = ComparacionObras.newBuilder()
            .setObraPrincipal(convertToObraProto(principal))
            .setObraSecundaria(convertToObraProto(secundaria))
            .setTecnica(tecnica)
            .setEstilo(estilo)
            .setEpoca(epoca)
            .setArtista(artista)
            .setTematica(tematica)
            .setIndiceSimilitud(indiceSimilitud)
            .setAnalisis(analisis)
            .addAllSimilitudesClave(similitudes)
            .addAllDiferenciasClave(diferencias)
            .build();

        responseObserver.onNext(comparacion);
        responseObserver.onCompleted();
    }

    @Override
    public void obtenerObrasSimilares(ObtenerObrasSimilaresRequest request,
                                      StreamObserver<ObrasSimilaresResponse> responseObserver) {
        Optional<ObraEntity> obraReferenciaOpt = obraRepository.findById(request.getObraId());

        if (obraReferenciaOpt.isEmpty()) {
            responseObserver.onError(
                io.grpc.Status.NOT_FOUND
                    .withDescription("Obra no encontrada: " + request.getObraId())
                    .asRuntimeException()
            );
            return;
        }

        ObraEntity referencia = obraReferenciaOpt.get();
        String criterio = request.getCriterio().toUpperCase();
        List<ObraEntity> similares = new ArrayList<>();
        String criterioBusqueda;

        switch (criterio) {
            case "ESTILO" -> {
                criterioBusqueda = "Estilo: " + referencia.getEstilo();
                similares = obraRepository.findByEstiloContainingIgnoreCase(referencia.getEstilo())
                    .stream()
                    .filter(o -> !o.getId().equals(referencia.getId()))
                    .sorted(Comparator.comparing(ObraEntity::getCalificacionPromedio).reversed())
                    .limit(request.getCantidad() > 0 ? request.getCantidad() : 5)
                    .collect(Collectors.toList());
            }
            case "TECNICA" -> {
                criterioBusqueda = "Técnica: " + referencia.getTecnica();
                similares = obraRepository.findByTecnicaContainingIgnoreCase(referencia.getTecnica())
                    .stream()
                    .filter(o -> !o.getId().equals(referencia.getId()))
                    .sorted(Comparator.comparing(ObraEntity::getCalificacionPromedio).reversed())
                    .limit(request.getCantidad() > 0 ? request.getCantidad() : 5)
                    .collect(Collectors.toList());
            }
            case "ARTISTA" -> {
                criterioBusqueda = "Artista: " + (referencia.getArtista() != null
                    ? referencia.getArtista().getNombre() : "Desconocido");
                if (referencia.getArtista() != null) {
                    similares = obraRepository.findByArtistaId(referencia.getArtista().getId())
                        .stream()
                        .filter(o -> !o.getId().equals(referencia.getId()))
                        .sorted(Comparator.comparing(ObraEntity::getAnio))
                        .limit(request.getCantidad() > 0 ? request.getCantidad() : 5)
                        .collect(Collectors.toList());
                }
            }
            case "EPOCA" -> {
                criterioBusqueda = "Época: " + referencia.getAnio();
                int rango = 50; // años de margen
                similares = obraRepository.findAll()
                    .stream()
                    .filter(o -> Math.abs(o.getAnio() - referencia.getAnio()) <= rango)
                    .filter(o -> !o.getId().equals(referencia.getId()))
                    .sorted(Comparator.comparing(ObraEntity::getAnio))
                    .limit(request.getCantidad() > 0 ? request.getCantidad() : 5)
                    .collect(Collectors.toList());
            }
            default -> {
                criterioBusqueda = "Calificación similar";
                similares = obraRepository.findAll()
                    .stream()
                    .filter(o -> !o.getId().equals(referencia.getId()))
                    .sorted(Comparator.comparingDouble(o ->
                        Math.abs(o.getCalificacionPromedio() - referencia.getCalificacionPromedio())))
                    .limit(request.getCantidad() > 0 ? request.getCantidad() : 5)
                    .collect(Collectors.toList());
            }
        }

        ObrasSimilaresResponse response = ObrasSimilaresResponse.newBuilder()
            .addAllObras(similares.stream()
                .map(this::convertToObraProto)
                .collect(Collectors.toList()))
            .setCriterioBusqueda(criterioBusqueda)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void analizarEstilo(AnalizarEstiloRequest request,
                             StreamObserver<AnalisisEstilo> responseObserver) {
        String estilo = request.getEstilo();

        // Buscar obras del estilo
        List<ObraEntity> obrasEstilo = obraRepository
            .findByEstiloContainingIgnoreCase(estilo);

        // Analizar características comunes
        Map<String, Long> tecnicasFrecuentes = obrasEstilo.stream()
            .collect(Collectors.groupingBy(ObraEntity::getTecnica, Collectors.counting()));

        Map<String, Long> epocasDistribucion = obrasEstilo.stream()
            .collect(Collectors.groupingBy(
                o -> String.valueOf((o.getAnio() / 25) * 25), // Agrupar por cuartos de siglo
                Collectors.counting()
            ));

        // Construir elementos del estilo
        List<ElementoEstilo> elementos = new ArrayList<>();

        // Elemento técnic
        String tecnicaMasFrecuente = tecnicasFrecuentes.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Variada");

        elementos.add(ElementoEstilo.newBuilder()
            .setNombre("Técnica predominante")
            .setDescripcion("Las obras del estilo " + estilo + " suelen emplear " + tecnicaMasFrecuente)
            .addAllObrasEjemplo(obrasEstilo.stream()
                .filter(o -> o.getTecnica().equals(tecnicaMasFrecuente))
                .limit(3)
                .map(ObraEntity::getTitulo)
                .collect(Collectors.toList()))
            .build());

        // Elemento época
        String epocaMayor = epocasDistribucion.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("varios periodos");

        elementos.add(ElementoEstilo.newBuilder()
            .setNombre("Período de mayor producción")
            .setDescripcion("Entre los años " + epocaMayor)
            .build());

        // Generar descripción
        String descripcion = String.format(
            "El estilo %s se caracteriza por el uso predominante de %s. " +
            "Su período de mayor auge fue entre los años %s. " +
            "En nuestra colección contamos con %d obras representativas de este estilo.",
            estilo, tecnicaMasFrecuente, epocaMayor, obrasEstilo.size()
        );

        // Características distinguibles
        List<String> caracteristicas = Arrays.asList(
            "Uso característico de " + tecnicaMasFrecuente,
            "Desarrollado principalmente en el siglo " + epocaMayor,
            "Enfoque en " + (obrasEstilo.size() > 0 ? obrasEstilo.get(0).getTecnica() : "la técnica"),
            "Representado por " + obrasEstilo.size() + " obras en la colección"
        );

        AnalisisEstilo analisis = AnalisisEstilo.newBuilder()
            .addAllElementos(elementos)
            .setDescripcionGeneral(descripcion)
            .addAllCaracteristicasDistinguibles(caracteristicas)
            .build();

        responseObserver.onNext(analisis);
        responseObserver.onCompleted();
    }

    // Métodos auxiliares de comparación
    private DimensionComparacion compararTecnica(ObraEntity a, ObraEntity b) {
        double similitud = calcularSimilitudTexto(
            a.getTecnica() != null ? a.getTecnica() : "",
            b.getTecnica() != null ? b.getTecnica() : ""
        );

        return DimensionComparacion.newBuilder()
            .setNombre("Técnica")
            .setValorObraPrincipal(a.getTecnica() != null ? a.getTecnica() : "No especificada")
            .setValorObraSecundaria(b.getTecnica() != null ? b.getTecnica() : "No especificada")
            .setSimilitud(similitud)
            .setObservacion(similitud > 70
                ? "Las obras emplean técnicas similares"
                : "Las técnicas son distintivas")
            .build();
    }

    private DimensionComparacion compararEstilo(ObraEntity a, ObraEntity b) {
        double similitud = calcularSimilitudTexto(
            a.getEstilo() != null ? a.getEstilo() : "",
            b.getEstilo() != null ? b.getEstilo() : ""
        );

        return DimensionComparacion.newBuilder()
            .setNombre("Estilo")
            .setValorObraPrincipal(a.getEstilo() != null ? a.getEstilo() : "No especificado")
            .setValorObraSecundaria(b.getEstilo() != null ? b.getEstilo() : "No especificado")
            .setSimilitud(similitud)
            .setObservacion(similitud > 70
                ? "Pertenecen al mismo movimiento artístico"
                : "Representan estilos diferentes")
            .build();
    }

    private DimensionComparacion compararEpoca(ObraEntity a, ObraEntity b) {
        int diferenciaAnios = Math.abs(a.getAnio() - b.getAnio());
        double similitud = Math.max(0, 100 - (diferenciaAnios * 2.0)); // 50 años = 0%

        return DimensionComparacion.newBuilder()
            .setNombre("Época")
            .setValorObraPrincipal(String.valueOf(a.getAnio()))
            .setValorObraSecundaria(String.valueOf(b.getAnio()))
            .setSimilitud(similitud)
            .setObservacion(diferenciaAnios < 25
                ? "Obras contemporáneas"
                : "Separadas por " + diferenciaAnios + " años")
            .build();
    }

    private DimensionComparacion compararArtista(ObraEntity a, ObraEntity b) {
        boolean mismoArtista = a.getArtista() != null && b.getArtista() != null
            && a.getArtista().getId().equals(b.getArtista().getId());

        double similitud = mismoArtista ? 100 : 0;

        return DimensionComparacion.newBuilder()
            .setNombre("Artista")
            .setValorObraPrincipal(a.getArtista() != null ? a.getArtista().getNombre() : "Desconocido")
            .setValorObraSecundaria(b.getArtista() != null ? b.getArtista().getNombre() : "Desconocido")
            .setSimilitud(similitud)
            .setObservacion(mismoArtista
                ? "Mismo autor"
                : "Artistas diferentes")
            .build();
    }

    private DimensionComparacion compararTematica(ObraEntity a, ObraEntity b) {
        // Análisis simple basado en descripción
        double similitud = calcularSimilitudTexto(
            a.getDescripcion() != null ? a.getDescripcion() : "",
            b.getDescripcion() != null ? b.getDescripcion() : ""
        );

        return DimensionComparacion.newBuilder()
            .setNombre("Temática")
            .setValorObraPrincipal("Analizado desde descripción")
            .setValorObraSecundaria("Analizado desde descripción")
            .setSimilitud(similitud)
            .setObservacion(similitud > 50
                ? "Temas relacionados"
                : "Enfoques temáticos distintos")
            .build();
    }

    private double calcularSimilitudTexto(String a, String b) {
        if (a.isEmpty() && b.isEmpty()) return 100;
        if (a.isEmpty() || b.isEmpty()) return 0;
        if (a.equalsIgnoreCase(b)) return 100;

        // Similitud de Jaccard simple
        Set<String> palabrasA = new HashSet<>(Arrays.asList(a.toLowerCase().split("\\s+")));
        Set<String> palabrasB = new HashSet<>(Arrays.asList(b.toLowerCase().split("\\s+")));

        Set<String> intersepcion = new HashSet<>(palabrasA);
        intersepcion.retainAll(palabrasB);

        Set<String> union = new HashSet<>(palabrasA);
        union.addAll(palabrasB);

        return union.isEmpty() ? 0 : (intersepcion.size() * 100.0) / union.size();
    }

    private double calcularIndiceSimilitud(double... similitudes) {
        return Arrays.stream(similitudes).average().orElse(0);
    }

    private String generarAnalisis(ObraEntity principal, ObraEntity secundaria, double indiceSimilitud) {
        StringBuilder analisis = new StringBuilder();

        analisis.append(String.format(
            "Comparación entre '%s' de %s y '%s' de %s. ",
            principal.getTitulo(),
            principal.getArtista() != null ? principal.getArtista().getNombre() : "autor desconocido",
            secundaria.getTitulo(),
            secundaria.getArtista() != null ? secundaria.getArtista().getNombre() : "autor desconocido"
        ));

        if (indiceSimilitud > 70) {
            analisis.append("Las obras presentan una fuerte similitud, compartiendo elementos comunes ");
            analisis.append("que sugieren influencias compartidas o pertenencia al mismo contexto artístico. ");
        } else if (indiceSimilitud > 40) {
            analisis.append("Las obras muestran algunos puntos de contacto, aunque mantienen ");
            analisis.append("características distintivas que las diferencian significativamente. ");
        } else {
            analisis.append("Las obras son notablemente diferentes, representando enfoques artísticos ");
            analisis.append("o contextos históricos distintos. ");
        }

        analisis.append(String.format(
            "El índice de similitud general es de %.1f%%.",
            indiceSimilitud
        ));

        return analisis.toString();
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
