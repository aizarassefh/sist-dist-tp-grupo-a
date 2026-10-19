package com.unla.museo.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para Recomendaciones (Hito 2).
 * Conecta con el microservicio NestJS via gRPC.
 */
@RestController
@RequestMapping("/api/v2/recomendaciones")
@RequiredArgsConstructor
@Slf4j
public class RecomendacionesController {

    // El cliente gRPC llamaría al servicio NestJS en puerto 9091

    /**
     * GET /api/v2/recomendaciones/personalizadas
     * Obtiene recomendaciones personalizadas para el usuario
     */
    @GetMapping("/personalizadas")
    public Mono<ResponseEntity<Map<String, Object>>> obtenerRecomendaciones(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long usuarioId,
            @RequestParam(required = false, defaultValue = "10") int cantidad,
            @RequestParam(required = false) String contexto) {
        log.info("Obteniendo recomendaciones para usuario: {}, cantidad: {}", usuarioId, cantidad);

        // Llamaría al servicio NestJS via gRPC
        // grpcRecomendacionesClient.obtenerRecomendaciones(request)

        return Mono.just(ResponseEntity.ok(Map.of(
            "recomendaciones", List.of(
                Map.of(
                    "obra", Map.of(
                        "id", 3,
                        "titulo", "Impresión, sol naciente",
                        "artista", Map.of(
                            "id", 2,
                            "nombre", "Claude Monet",
                            "nacionalidad", "Francesa"
                        ),
                        "estilo", "Impresionismo",
                        "tecnica", "Óleo sobre lienzo",
                        "imagenUrl", "/obras/sol-naciente.jpg",
                        "calificacionPromedio", 4.8
                    ),
                    "score", 94.5,
                    "razon", "Basado en tu interés en Impresionismo",
                    "tipoRecomendacion", "PERSONAL"
                ),
                Map.of(
                    "obra", Map.of(
                        "id", 8,
                        "titulo", "El caminante sobre el mar de nubes",
                        "artista", Map.of(
                            "id", 4,
                            "nombre", "Caspar David Friedrich",
                            "nacionalidad", "Alemana"
                        ),
                        "estilo", "Romanticismo",
                        "tecnica", "Óleo sobre lienzo",
                        "imagenUrl", "/obras/caminante.jpg",
                        "calificacionPromedio", 4.7
                    ),
                    "score", 87.2,
                    "razon", "Coincide con tus técnicas favoritas: Óleo",
                    "tipoRecomendacion", "PERSONAL"
                ),
                Map.of(
                    "obra", Map.of(
                        "id", 15,
                        "titulo", "El beso",
                        "artista", Map.of(
                            "id", 5,
                            "nombre", "Gustav Klimt",
                            "nacionalidad", "Austriaca"
                        ),
                        "estilo", "Art Nouveau",
                        "tecnica", "Óleo y pan de oro",
                        "imagenUrl", "/obras/beso.jpg",
                        "calificacionPromedio", 4.9
                    ),
                    "score", 82.1,
                    "razon", "Popular entre usuarios con gustos similares",
                    "tipoRecomendacion", "COLABORATIVA"
                )
            ),
            "preferenciasUsadas", Map.of(
                "estilosFavoritos", List.of("Impresionismo", "Post-Impresionismo"),
                "tecnicasFavoritas", List.of("Óleo", "Témpera"),
                "artistasFavoritos", List.of(1, 2, 3),
                "umbralCalificacion", 4.0
            ),
            "totalDisponibles", 25
        )));
    }

    /**
     * GET /api/v2/recomendaciones/contextuales
     * Obtiene sugerencias basadas en la obra actual
     */
    @GetMapping("/contextuales")
    public Mono<ResponseEntity<Map<String, Object>>> obtenerSugerenciasContextuales(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long usuarioId,
            @RequestParam Long obraActualId,
            @RequestParam(required = false, defaultValue = "SIGUIENTE_OBRA") String tipoSugerencia,
            @RequestParam(required = false, defaultValue = "5") int cantidad) {
        log.info("Obteniendo sugerencias contextuales para obra: {}, tipo: {}", obraActualId, tipoSugerencia);

        String mensaje = switch (tipoSugerencia) {
            case "MISMO_ESTILO" -> "Otras obras del mismo estilo";
            case "MISMO_ARTISTA" -> "Más obras del mismo artista";
            case "COMPLETAR_VISITA" -> "Para completar tu visita";
            default -> "Siguiente obra recomendada";
        };

        return Mono.just(ResponseEntity.ok(Map.of(
            "obraActual", Map.of(
                "id", obraActualId,
                "titulo", "Obra actual"
            ),
            "recomendaciones", List.of(
                Map.of(
                    "obra", Map.of(
                        "id", obraActualId + 1,
                        "titulo", "Obra recomendada " + tipoSugerencia.toLowerCase().replace("_", " "),
                        "estilo", "Impresionismo",
                        "imagenUrl", "/obras/recomendada-1.jpg"
                    ),
                    "score", 92.0,
                    "razon", mensaje,
                    "tipoRecomendacion", tipoSugerencia
                ),
                Map.of(
                    "obra", Map.of(
                        "id", obraActualId + 2,
                        "titulo", "Otra obra relacionada",
                        "estilo", "Impresionismo",
                        "imagenUrl", "/obras/recomendada-2.jpg"
                    ),
                    "score", 85.5,
                    "razon", "Usuarios similares también vieron",
                    "tipoRecomendacion", tipoSugerencia
                )
            ),
            "totalDisponibles", cantidad
        )));
    }

    /**
     * GET /api/v2/recomendaciones/populares
     * Obtiene las obras más populares
     */
    @GetMapping("/populares")
    public Mono<ResponseEntity<Map<String, Object>>> obtenerObrasPopulares(
            @RequestParam(required = false, defaultValue = "mes") String periodo,
            @RequestParam(required = false, defaultValue = "10") int cantidad,
            @RequestParam(required = false) String filtroEstilo) {
        log.info("Obteniendo obras populares - periodo: {}, cantidad: {}", periodo, cantidad);

        String subtitulo = filtroEstilo != null
            ? "Las obras de " + filtroEstilo + " más visitadas"
            : "Las obras más visitadas de este " + periodo;

        return Mono.just(ResponseEntity.ok(Map.of(
            "obras", List.of(
                Map.of(
                    "obra", Map.of(
                        "id", 12,
                        "titulo", "La Gioconda",
                        "artista", Map.of("nombre", "Leonardo da Vinci"),
                        "estilo", "Renacimiento",
                        "imagenUrl", "/obras/gioconda.jpg"
                    ),
                    "visitas", 12543,
                    "calificacionPromedio", 4.9,
                    "posicionRanking", 1
                ),
                Map.of(
                    "obra", Map.of(
                        "id", 5,
                        "titulo", "La persistencia de la memoria",
                        "artista", Map.of("nombre", "Salvador Dalí"),
                        "estilo", "Surrealismo",
                        "imagenUrl", "/obras/ persistencia.jpg"
                    ),
                    "visitas", 11234,
                    "calificacionPromedio", 4.8,
                    "posicionRanking", 2
                ),
                Map.of(
                    "obra", Map.of(
                        "id", 1,
                        "titulo", "Noche estrellada",
                        "artista", Map.of("nombre", "Vincent van Gogh"),
                        "estilo", "Post-Impresionismo",
                        "imagenUrl", "/obras/noche-estrellada.jpg"
                    ),
                    "visitas", 9876,
                    "calificacionPromedio", 4.8,
                    "posicionRanking", 3
                )
            ),
            "periodo", periodo,
            "subtitulo", subtitulo
        )));
    }

    /**
     * GET /api/v2/recomendaciones/tendencias
     * Obtiene obras en tendencia
     */
    @GetMapping("/tendencias")
    public Mono<ResponseEntity<Map<String, Object>>> obtenerObrasTendencia(
            @RequestParam(required = false, defaultValue = "10") int cantidad,
            @RequestParam(required = false, defaultValue = "semana") String periodoComparacion) {
        log.info("Obteniendo obras en tendencia - cantidad: {}", cantidad);

        return Mono.just(ResponseEntity.ok(Map.of(
            "obras", List.of(
                Map.of(
                    "obra", Map.of(
                        "id", 42,
                        "titulo", "La joven de la perla",
                        "artista", Map.of("nombre", "Johannes Vermeer"),
                        "estilo", "Barroco",
                        "imagenUrl", "/obras/perla.jpg"
                    ),
                    "variacionVisitas", 145.5,
                    "nuevosComentarios", 28,
                    "scoreTendencia", 89.5
                ),
                Map.of(
                    "obra", Map.of(
                        "id", 23,
                        "titulo", "La gran ola de Kanagawa",
                        "artista", Map.of("nombre", "Hokusai"),
                        "estilo", "Ukiyo-e",
                        "imagenUrl", "/obras/ola.jpg"
                    ),
                    "variacionVisitas", 123.8,
                    "nuevosComentarios", 35,
                    "scoreTendencia", 87.2
                ),
                Map.of(
                    "obra", Map.of(
                        "id", 17,
                        "titulo", "Guernica",
                        "artista", Map.of("nombre", "Pablo Picasso"),
                        "estilo", "Cubismo",
                        "imagenUrl", "/obras/guernica.jpg"
                    ),
                    "variacionVisitas", 98.4,
                    "nuevosComentarios", 42,
                    "scoreTendencia", 82.1
                )
            ),
            "tendenciaCalculada", "Obras del período Barroco en auge"
        )));
    }

    /**
     * GET /api/v2/recomendaciones/{usuarioId}/historial
     * Obtiene recomendaciones basadas en el historial del usuario via NestJS
     */
    @GetMapping("/{usuarioId}/historial")
    public Mono<ResponseEntity<Map<String, Object>>> obtenerRecomendacionesHistorial(
            @PathVariable Long usuarioId,
            @RequestParam(required = false, defaultValue = "10") int cantidad) {
        log.info("Obteniendo recomendaciones basadas en historial para usuario: {}", usuarioId);

        // Esto llamaría al servicio NestJS via gRPC usando el historial

        return Mono.just(ResponseEntity.ok(Map.of(
            "recomendaciones", List.of(
                Map.of(
                    "obra", Map.of(
                        "id", 33,
                        "titulo", "Las meninas",
                        "artista", Map.of("nombre", "Diego Velázquez"),
                        "estilo", "Barroco",
                        "imagenUrl", "/obras/meninas.jpg"
                    ),
                    "score", 91.2,
                    "razon", "Complementa tu interés en retratos del siglo XVII",
                    "tipoRecomendacion", "HISTORIAL"
                )
            ),
            "basadoEnHistorial", true,
            "totalObrasVisitadas", 15,
            "totalDisponibles", 8
        )));
    }
}
