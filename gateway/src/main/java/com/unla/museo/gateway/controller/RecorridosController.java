package com.unla.museo.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Controller REST para Recorridos Guiados (Hito 2).
 * Expone endpoints REST que internamente usan gRPC.
 */
@RestController
@RequestMapping("/api/v2/recorridos")
@RequiredArgsConstructor
@Slf4j
public class RecorridosController {

    // Clientes gRPC serán inyectados automáticamente
    // Por ahora retornamos respuestas simuladas

    /**
     * POST /api/v2/recorridos/generar
     * Genera un recorrido personalizado
     */
    @PostMapping("/generar")
    public Mono<ResponseEntity<Map<String, Object>>> generarRecorrido(
            @RequestBody Map<String, Object> criterios) {
        log.info("Generando recorrido con criterios: {}", criterios);

        // Aquí se llamaría al servicio gRPC de Spring Boot
        // grpcRecorridosService.generarRecorrido(request)

        return Mono.just(ResponseEntity.ok(Map.of(
            "id", System.currentTimeMillis(),
            "nombre", "Recorrido personalizado",
            "descripcion", "Recorrido generado según sus preferencias",
            "duracionEstimadaMinutos", 60,
            "distanciaTotalMetros", 450.5,
            "tema", criterios.getOrDefault("tipo", "Personalizado"),
            "paradas", java.util.List.of(
                Map.of("orden", 1, "obraId", 1, "tiempoSugeridoMinutos", 10),
                Map.of("orden", 2, "obraId", 5, "tiempoSugeridoMinutos", 8),
                Map.of("orden", 3, "obraId", 12, "tiempoSugeridoMinutos", 12)
            ),
            "compartible", true
        )));
    }

    /**
     * GET /api/v2/recorridos
     * Lista los recorridos del usuario
     */
    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> listarRecorridos(
            @RequestParam(required = false, defaultValue = "0") int pagina,
            @RequestParam(required = false, defaultValue = "10") int tamano) {
        log.info("Listando recorridos - página: {}, tamaño: {}", pagina, tamano);

        return Mono.just(ResponseEntity.ok(Map.of(
            "recorridos", java.util.List.of(
                Map.of(
                    "id", 1L,
                    "nombre", "Descubriendo el Impresionismo",
                    "descripcion", "Obras maestras del Impresionismo",
                    "duracionEstimadaMinutos", 45,
                    "tema", "Impresionismo",
                    "fechaCreacion", "2024-01-15T10:00:00"
                ),
                Map.of(
                    "id", 2L,
                    "nombre", "Los grandes maestros",
                    "descripcion", "Visita a obras de artistas destacados",
                    "duracionEstimadaMinutos", 60,
                    "tema", "Grandes Maestros",
                    "fechaCreacion", "2024-01-20T15:30:00"
                )
            ),
            "total", 2,
            "pagina", pagina,
            "tamanio", tamano
        )));
    }

    /**
     * GET /api/v2/recorridos/{id}
     * Obtiene un recorrido específico
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> obtenerRecorrido(
            @PathVariable Long id) {
        log.info("Obteniendo recorrido: {}", id);

        return Mono.just(ResponseEntity.ok(Map.of(
            "id", id,
            "nombre", "Recorrido del día",
            "descripcion", "Recorrido personalizado",
            "duracionEstimadaMinutos", 30,
            "distanciaTotalMetros", 250.0,
            "tema", "Ecléctico",
            "paradas", java.util.List.of(
                Map.of(
                    "orden", 1,
                    "obra", Map.of(
                        "id", 1,
                        "titulo", "Noche estrellada",
                        "artista", Map.of("nombre", "Van Gogh")
                    ),
                    "tiempoSugeridoMinutos", 10,
                    "notas", "Obra emblemática del post-impresionismo"
                ),
                Map.of(
                    "orden", 2,
                    "obra", Map.of(
                        "id", 5,
                        "titulo", "La persistencia de la memoria",
                        "artista", Map.of("nombre", "Dalí")
                    ),
                    "tiempoSugeridoMinutos", 8,
                    "notas", "Surrealismo puro"
                )
            ),
            "compartible", true
        )));
    }

    /**
     * DELETE /api/v2/recorridos/{id}
     * Elimina un recorrido
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> eliminarRecorrido(
            @PathVariable Long id) {
        log.info("Eliminando recorrido: {}", id);

        // Aquí se llamaría al servicio gRPC para eliminar

        return Mono.just(ResponseEntity.ok(Map.of(
            "exito", true,
            "mensaje", "Recorrido eliminado correctamente"
        )));
    }

    /**
     * POST /api/v2/recorridos/{id}/compartir
     * Comparte un recorrido
     */
    @PostMapping("/{id}/compartir")
    public Mono<ResponseEntity<Map<String, Object>>> compartirRecorrido(
            @PathVariable Long id) {
        log.info("Compartiendo recorrido: {}", id);

        String codigoCompartir = "RCD-" + id + "-" + System.currentTimeMillis();

        return Mono.just(ResponseEntity.ok(Map.of(
            "exito", true,
            "mensaje", "Recorrido compartido exitosamente",
            "codigoCompartir", codigoCompartir,
            "url", "/recorrido/compartido/" + codigoCompartir
        )));
    }
}
