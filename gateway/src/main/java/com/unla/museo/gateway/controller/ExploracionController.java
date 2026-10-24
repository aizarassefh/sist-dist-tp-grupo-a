package com.unla.museo.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para Exploración del Catálogo (Hito 2).
 * Orquesta llamadas gRPC al backend.
 */
@RestController
@RequestMapping("/api/v2/explorar")
@RequiredArgsConstructor
@Slf4j
public class ExploracionController {

    /**
     * POST /api/v2/explorar/espacio
     * Explora el catálogo en modo espacial/visual
     */
    @PostMapping("/espacio")
    public Mono<ResponseEntity<Map<String, Object>>> explorarEspacio(
            @RequestBody Map<String, Object> request) {
        String modo = (String) request.getOrDefault("modo", "ESTILOS");
        Long usuarioId = request.get("usuarioId") != null
            ? Long.valueOf(request.get("usuarioId").toString())
            : null;

        log.info("Explorando espacio en modo: {}", modo);

        // Aquí se llamaría al servicio gRPC explorarEspacio

        return Mono.just(ResponseEntity.ok(Map.of(
            "espacio", Map.of(
                "clusters", List.of(
                    Map.of(
                        "id", "cluster-1",
                        "nombre", "Impresionismo",
                        "categoria", "estilo",
                        "totalObras", 45,
                        "x", 25.5,
                        "y", 30.2,
                        "color", "#FF6B6B",
                        "obrasMuestra", List.of(
                            Map.of("id", 1, "titulo", "Sol naciente"),
                            Map.of("id", 2, "titulo", "Almendro en flor")
                        )
                    ),
                    Map.of(
                        "id", "cluster-2",
                        "nombre", "Surrealismo",
                        "categoria", "estilo",
                        "totalObras", 28,
                        "x", 75.3,
                        "y", 45.8,
                        "color", "#9B59B6",
                        "obrasMuestra", List.of(
                            Map.of("id", 3, "titulo", "La persistencia de la memoria")
                        )
                    ),
                    Map.of(
                        "id", "cluster-3",
                        "nombre", "Renacimiento",
                        "categoria", "estilo",
                        "totalObras", 62,
                        "x", 40.1,
                        "y", 80.6,
                        "color", "#3498DB",
                        "obrasMuestra", List.of(
                            Map.of("id", 4, "titulo", "La Gioconda")
                        )
                    )
                ),
                "artistasDestacados", List.of(
                    Map.of(
                        "id", 1,
                        "nombre", "Vincent van Gogh",
                        "nacionalidad", "Neerlandés"
                    ),
                    Map.of(
                        "id", 2,
                        "nombre", "Leonardo da Vinci",
                        "nacionalidad", "Italiano"
                    )
                ),
                "estilos", List.of(
                    Map.of(
                        "nombre", "Impresionismo",
                        "cantidadObras", 45,
                        "periodoInicio", 1860,
                        "periodoFin", 1900,
                        "descripcion", "Movimiento artístico del siglo XIX"
                    ),
                    Map.of(
                        "nombre", "Surrealismo",
                        "cantidadObras", 28,
                        "periodoInicio", 1920,
                        "periodoFin", 1950,
                        "descripcion", "Exploración del inconsciente"
                    )
                ),
                "epocas", List.of(
                    Map.of(
                        "nombre", "Renacimiento",
                        "rangoAnios", "1400-1600",
                        "cantidadObras", 62,
                        "estilosPredominantes", List.of("Renacentista", "Clasicismo")
                    )
                )
            ),
            "sugerenciasNavegacion", List.of(
                "Explora el cluster de Impresionismo",
                "Descubre artistas del Renacimiento",
                "Recorre la cronología del arte"
            ),
            "modo", modo
        )));
    }

    /**
     * POST /api/v2/explorar/descubrir
     * Descubre obras con filtros dinámicos
     */
    @PostMapping("/descubrir")
    public Mono<ResponseEntity<Map<String, Object>>> descubrirObras(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> filtros = (List<Map<String, String>>) request.getOrDefault("filtros", List.of());
        String ordenarPor = (String) request.getOrDefault("ordenarPor", "id");
        boolean ordenDescendente = (boolean) request.getOrDefault("ordenDescendente", false);
        int pagina = request.get("pagina") != null
            ? Integer.parseInt(request.get("pagina").toString())
            : 0;
        int tamano = request.get("tamano") != null
            ? Integer.parseInt(request.get("tamano").toString())
            : 10;

        log.info("Descubriendo obras con {} filtros", filtros.size());

        return Mono.just(ResponseEntity.ok(Map.of(
            "obras", List.of(
                Map.of(
                    "id", 15,
                    "titulo", "El jardín de las delicias",
                    "descripcion", "Tríptico del Bosco",
                    "estilo", "Renacimiento",
                    "tecnica", "Óleo sobre tabla",
                    "anio", 1500,
                    "artista", Map.of("id", 10, "nombre", "El Bosco"),
                    "imagenUrl", "/obras/jardin-delicias.jpg",
                    "calificacionPromedio", 4.6
                ),
                Map.of(
                    "id", 22,
                    "titulo", "La bacanal de los andrios",
                    "descripcion", "Escena mitológica",
                    "estilo", "Renacimiento",
                    "tecnica", "Óleo sobre lienzo",
                    "anio", 1518,
                    "artista", Map.of("id", 11, "nombre", "Tiziano"),
                    "imagenUrl", "/obras/bacanal.jpg",
                    "calificacionPromedio", 4.4
                )
            ),
            "total", 150,
            "pagina", pagina,
            "tamanio", tamano,
            "filtrosAplicados", filtros,
            "sugerenciasFiltros", List.of(
                "Añadir filtro: siglo XVI",
                "Ordenar por popularidad",
                "Filtrar por técnica: Fresco"
            ),
            "ordenarPor", ordenarPor,
            "ordenDescendente", ordenDescendente
        )));
    }

    /**
     * GET /api/v2/explorar/cronologia
     * Explora el catálogo cronológicamente
     */
    @GetMapping("/cronologia")
    public Mono<ResponseEntity<Map<String, Object>>> explorarCronologia(
            @RequestParam(required = false) Integer anioInicio,
            @RequestParam(required = false) Integer anioFin,
            @RequestParam(required = false) String filtroEstilo) {
        log.info("Explorando cronología: {} - {}", anioInicio, anioFin);

        int inicio = anioInicio != null ? anioInicio : 1400;
        int fin = anioFin != null ? anioFin : 2000;

        return Mono.just(ResponseEntity.ok(Map.of(
            "items", List.of(
                Map.of(
                    "periodo", "Siglo XV",
                    "anioInicio", 1400,
                    "anioFin", 1499,
                    "cantidadObras", 12,
                    "obrasDestacadas", List.of(
                        Map.of(
                            "id", 50,
                            "titulo", "Anunciación",
                            "artista", Map.of("nombre", "Fra Angelico")
                        ),
                        Map.of(
                            "id", 51,
                            "titulo", "La Virgen de las Rocas",
                            "artista", Map.of("nombre", "Leonardo da Vinci")
                        )
                    ),
                    "hechosRelevantes", List.of(
                        "Invención de la imprenta por Gutenberg",
                        "Caída de Constantinopla"
                    )
                ),
                Map.of(
                    "periodo", "Siglo XVI",
                    "anioInicio", 1500,
                    "anioFin", 1599,
                    "cantidadObras", 28,
                    "obrasDestacadas", List.of(
                        Map.of(
                            "id", 52,
                            "titulo", "La escuela de Atenas",
                            "artista", Map.of("nombre", "Rafael")
                        ),
                        Map.of(
                            "id", 53,
                            "titulo", "La Gioconda",
                            "artista", Map.of("nombre", "Leonardo da Vinci")
                        )
                    ),
                    "hechosRelevantes", List.of(
                        "La Reforma Protestante",
                        "Contrarreforma Católica"
                    )
                ),
                Map.of(
                    "periodo", "Años 1880-1889",
                    "anioInicio", 1880,
                    "anioFin", 1889,
                    "cantidadObras", 35,
                    "obrasDestacadas", List.of(
                        Map.of(
                            "id", 1,
                            "titulo", "Noche estrellada",
                            "artista", Map.of("nombre", "Van Gogh")
                        ),
                        Map.of(
                            "id", 2,
                            "titulo", "Impresión, sol naciente",
                            "artista", Map.of("nombre", "Monet")
                        )
                    ),
                    "hechosRelevantes", List.of(
                        "El nacimiento del Impresionismo",
                        "Van Gogh en Arles"
                    )
                )
            ),
            "lineaTematicaDetectada", "Evolución del Renacimiento al Impresionismo",
            "rangoConsultado", inicio + " - " + fin
        )));
    }

    /**
     * GET /api/v2/explorar/conexiones/{obraId}
     * Obtiene las conexiones de una obra con otras
     */
    @GetMapping("/conexiones/{obraId}")
    public Mono<ResponseEntity<Map<String, Object>>> obtenerConexionesObras(
            @PathVariable Long obraId,
            @RequestParam(required = false, defaultValue = "1") int profundidad,
            @RequestParam(required = false) String tipoFiltro) {
        log.info("Obteniendo conexiones para obra: {}, profundidad: {}", obraId, profundidad);

        return Mono.just(ResponseEntity.ok(Map.of(
            "obraOrigen", Map.of(
                "id", obraId,
                "titulo", "Noche estrellada",
                "estilo", "Post-Impresionismo"
            ),
            "conexiones", List.of(
                Map.of(
                    "obraOrigenId", obraId,
                    "obraDestinoId", 3,
                    "tipoConexion", "MISMO_ESTILO",
                    "fuerza", 0.8,
                    "descripcion", "Comparten el estilo Post-Impresionismo"
                ),
                Map.of(
                    "obraOrigenId", obraId,
                    "obraDestinoId", 5,
                    "tipoConexion", "MISMO_ARTISTA",
                    "fuerza", 1.0,
                    "descripcion", "Ambas son de Vincent van Gogh"
                ),
                Map.of(
                    "obraOrigenId", obraId,
                    "obraDestinoId", 7,
                    "tipoConexion", "CONTEMPORANEAS",
                    "fuerza", 0.6,
                    "descripcion", "Obras contemporáneas (década de 1880)"
                )
            ),
            "obrasRelacionadas", List.of(
                Map.of(
                    "id", 3,
                    "titulo", "Autorretrato",
                    "artista", Map.of("nombre", "Van Gogh"),
                    "estilo", "Post-Impresionismo"
                ),
                Map.of(
                    "id", 5,
                    "titulo", "Los girasoles",
                    "artista", Map.of("nombre", "Van Gogh"),
                    "estilo", "Post-Impresionismo"
                ),
                Map.of(
                    "id", 7,
                    "titulo", "El almendro en flor",
                    "artista", Map.of("nombre", "Van Gogh"),
                    "estilo", "Post-Impresionismo"
                )
            ),
            "obrasTotalesConectadas", 3,
            "profundidad", profundidad
        )));
    }

    /**
     * GET /api/v2/explorar/estadisticas
     * Obtiene estadísticas del catálogo
     */
    @GetMapping("/estadisticas")
    public Mono<ResponseEntity<Map<String, Object>>> obtenerEstadisticas(
            @RequestParam(required = false, defaultValue = "true") boolean incluirDistribucion,
            @RequestParam(required = false, defaultValue = "false") boolean incluirTendencias,
            @RequestParam(required = false) String filtroEstilo) {
        log.info("Obteniendo estadísticas del catálogo");

        return Mono.just(ResponseEntity.ok(Map.of(
            "totalObras", 245,
            "totalArtistas", 48,
            "estilos", List.of(
                Map.of("estilo", "Renacimiento", "cantidad", 62, "porcentaje", 25.3),
                Map.of("estilo", "Impresionismo", "cantidad", 45, "porcentaje", 18.4),
                Map.of("estilo", "Barroco", "cantidad", 38, "porcentaje", 15.5),
                Map.of("estilo", "Surrealismo", "cantidad", 28, "porcentaje", 11.4),
                Map.of("estilo", "Post-Impresionismo", "cantidad", 25, "porcentaje", 10.2),
                Map.of("estilo", "Cubismo", "cantidad", 22, "porcentaje", 9.0),
                Map.of("estilo", "Otros", "cantidad", 25, "porcentaje", 10.2)
            ),
            "tecnicas", List.of(
                Map.of("tecnica", "Óleo sobre lienzo", "cantidad", 145),
                Map.of("tecnica", "Óleo sobre tabla", "cantidad", 48),
                Map.of("tecnica", "Fresco", "cantidad", 32),
                Map.of("tecnica", "Témpera", "cantidad", 12),
                Map.of("tecnica", "Escultura", "cantidad", 8)
            ),
            "periodos", List.of(
                Map.of("rango", "1400 - 1499", "cantidad", 12),
                Map.of("rango", "1500 - 1599", "cantidad", 35),
                Map.of("rango", "1600 - 1699", "cantidad", 42),
                Map.of("rango", "1700 - 1799", "cantidad", 28),
                Map.of("rango", "1800 - 1899", "cantidad", 68),
                Map.of("rango", "1900 - 1999", "cantidad", 52),
                Map.of("rango", "2000 - presente", "cantidad", 8)
            ),
            "calificacionPromedioGlobal", 4.3,
            "totalComentarios", 1254
        )));
    }

    /**
     * GET /api/v2/explorar/buscar
     * Búsqueda global en el catálogo
     */
    @GetMapping("/buscar")
    public Mono<ResponseEntity<Map<String, Object>>> busquedaGlobal(
            @RequestParam(required = false, defaultValue = "") String termino,
            @RequestParam(required = false) List<String> tipos,
            @RequestParam(required = false, defaultValue = "0") int pagina,
            @RequestParam(required = false, defaultValue = "20") int tamano) {
        log.info("Búsqueda global: '{}' en tipos {}", termino, tipos);

        return Mono.just(ResponseEntity.ok(Map.of(
            "resultados", List.of(
                Map.of(
                    "tipo", "OBRA",
                    "obra", Map.of(
                        "id", 1,
                        "titulo", "Noche estrellada",
                        "artista", Map.of("nombre", "Vincent van Gogh"),
                        "imagenUrl", "/obras/noche-estrellada.jpg"
                    ),
                    "relevancia", 98.5
                ),
                Map.of(
                    "tipo", "ARTISTA",
                    "artista", Map.of(
                        "id", 1,
                        "nombre", "Vincent van Gogh",
                        "nacionalidad", "Neerlandés"
                    ),
                    "relevancia", 92.3
                ),
                Map.of(
                    "tipo", "EVENTO",
                    "evento", Map.of(
                        "id", 10,
                        "titulo", "Van Gogh: La vida en colores",
                        "fechaInicio", "2024-03-15T10:00:00",
                        "ubicacion", "Sala Principal"
                    ),
                    "relevancia", 85.7
                )
            ),
            "totalObras", 15,
            "totalArtistas", 3,
            "totalEventos", 2,
            "termino", termino
        )));
    }
}
