package com.unla.museo.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para Comparación de Obras (Hito 2).
 */
@RestController
@RequestMapping("/api/v2/comparar")
@RequiredArgsConstructor
@Slf4j
public class ComparacionController {

    /**
     * GET /api/v2/comparar
     * Compara dos obras lado a lado
     */
    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> compararObras(
            @RequestParam Long obraPrincipalId,
            @RequestParam Long obraSecundariaId) {
        log.info("Comparando obra {} con obra {}", obraPrincipalId, obraSecundariaId);

        // Aquí se llamaría al servicio gRPC de Comparación

        return Mono.just(ResponseEntity.ok(Map.of(
            "obraPrincipal", Map.of(
                "id", obraPrincipalId,
                "titulo", "La noche estrellada",
                "artista", Map.of("nombre", "Vincent van Gogh"),
                "estilo", "Post-Impresionismo",
                "tecnica", "Óleo sobre lienzo",
                "anio", 1889,
                "imagenUrl", "/obras/noche-estrellada.jpg"
            ),
            "obraSecundaria", Map.of(
                "id", obraSecundariaId,
                "titulo", "Los girasoles",
                "artista", Map.of("nombre", "Vincent van Gogh"),
                "estilo", "Post-Impresionismo",
                "tecnica", "Óleo sobre lienzo",
                "anio", 1888,
                "imagenUrl", "/obras/girasoles.jpg"
            ),
            "tecnica", Map.of(
                "nombre", "Técnica",
                "valorObraPrincipal", "Óleo sobre lienzo",
                "valorObraSecundaria", "Óleo sobre lienzo",
                "similitud", 100.0,
                "observacion", "Ambas obras emplean la misma técnica"
            ),
            "estilo", Map.of(
                "nombre", "Estilo",
                "valorObraPrincipal", "Post-Impresionismo",
                "valorObraSecundaria", "Post-Impresionismo",
                "similitud", 95.0,
                "observacion", "Pertenecen al mismo movimiento artístico"
            ),
            "artista", Map.of(
                "nombre", "Artista",
                "valorObraPrincipal", "Vincent van Gogh",
                "valorObraSecundaria", "Vincent van Gogh",
                "similitud", 100.0,
                "observacion", "Mismo autor"
            ),
            "epoca", Map.of(
                "nombre", "Época",
                "valorObraPrincipal", "1889",
                "valorObraSecundaria", "1888",
                "similitud", 90.0,
                "observacion", "Obras contemporáneas (1 año de diferencia)"
            ),
            "indiceSimilitud", 96.25,
            "analisis", "Ambas obras pertenecen a la serie de Van Gogh, creadas durante su " +
                "período más fértil en Arles. Comparten técnica, estilo y autor, " +
                "diferenciándose principalmente en temática y momento específico de creación.",
            "similitudesClave", List.of(
                "Mismo artista: Vincent van Gogh",
                "Misma técnica: Óleo sobre lienzo",
                "Mismo movimiento: Post-Impresionismo",
                "Período creativo similar"
            ),
            "diferenciasClave", List.of(
                "Temática: Cielo nocturno vs naturaleza muerta",
                "Paleta cromática predominante",
                "Composición circular vs vertical"
            )
        )));
    }

    /**
     * GET /api/v2/comparar/similares
     * Obtiene obras similares a una referencia
     */
    @GetMapping("/similares")
    public Mono<ResponseEntity<Map<String, Object>>> obtenerObrasSimilares(
            @RequestParam Long obraId,
            @RequestParam(required = false, defaultValue = "ESTILO") String criterio,
            @RequestParam(required = false, defaultValue = "5") int cantidad) {
        log.info("Buscando obras similares a {} por criterio {}", obraId, criterio);

        return Mono.just(ResponseEntity.ok(Map.of(
            "obraReferenciaId", obraId,
            "criterioBusqueda", "Estilo: Post-Impresionismo",
            "obras", List.of(
                Map.of(
                    "id", 3,
                    "titulo", "Los olivos",
                    "artista", Map.of("nombre", "Vincent van Gogh"),
                    "estilo", "Post-Impresionismo",
                    "imagenUrl", "/obras/olivos.jpg",
                    "similitud", 98.5
                ),
                Map.of(
                    "id", 7,
                    "titulo", "El café de noche",
                    "artista", Map.of("nombre", "Vincent van Gogh"),
                    "estilo", "Post-Impresionismo",
                    "imagenUrl", "/obras/cafe.jpg",
                    "similitud", 95.0
                ),
                Map.of(
                    "id", 12,
                    "titulo", "La habitación en Arles",
                    "artista", Map.of("nombre", "Vincent van Gogh"),
                    "estilo", "Post-Impresionismo",
                    "imagenUrl", "/obras/habitacion.jpg",
                    "similitud", 92.5
                )
            )
        )));
    }

    /**
     * GET /api/v2/comparar/estilo/{estilo}/analizar
     * Analiza las características de un estilo artístico
     */
    @GetMapping("/estilo/{estilo}/analizar")
    public Mono<ResponseEntity<Map<String, Object>>> analizarEstilo(
            @PathVariable String estilo) {
        log.info("Analizando estilo: {}", estilo);

        return Mono.just(ResponseEntity.ok(Map.of(
            "estilo", estilo,
            "elementos", List.of(
                Map.of(
                    "nombre", "Técnica predominante",
                    "descripcion", "El óleo es la técnica más empleada en este estilo, permitiendo " +
                        "la superposición de capas y el juego de texturas",
                    "obrasEjemplo", List.of("Noche estrellada", "Los girasoles")
                ),
                Map.of(
                    "nombre", "Paleta cromática",
                    "descripcion", "Colores vivos y puros aplicados directamente desde el tubo",
                    "obrasEjemplo", List.of("Almendro en flor")
                ),
                Map.of(
                    "nombre", "Composición",
                    "descripcion", "Composiciones dinámicas con pinceladas expresivas y " +
                        "movimiento rotacional",
                    "obrasEjemplo", List.of("El sembrador")
                )
            ),
            "descripcionGeneral", estilo + " es un movimiento artístico que surgió como " +
                "reacción contra el Impresionismo, enfatizando la expresión emocional sobre " +
                "la representación naturalista de la luz y el color.",
            "caracteristicasDistinguibles", List.of(
                "Uso de colores puros sin mezclar",
                "Pinceladas visibles y expresivas",
                "Formas simplificadas y estilizadas",
                "Énfasis en la estructura y composición",
                "Expresión subjetiva y emocional"
            )
        )));
    }

    /**
     * POST /api/v2/comparar/coleccion
     * Compara una obra con toda una colección
     */
    @PostMapping("/coleccion")
    public Mono<ResponseEntity<Map<String, Object>>> compararConColeccion(
            @RequestBody Map<String, Object> request) {
        Long obraId = Long.valueOf(request.get("obraId").toString());
        log.info("Comparando obra {} con colección", obraId);

        return Mono.just(ResponseEntity.ok(Map.of(
            "obraPrincipal", Map.of(
                "id", obraId,
                "titulo", "Obra seleccionada",
                "estilo", "Impresionismo"
            ),
            "comparaciones", List.of(
                Map.of(
                    "obraId", 1,
                    "titulo", "Obra similar 1",
                    "similitud", 85.5,
                    "dimensionesSimilares", List.of("Estilo", "Época")
                ),
                Map.of(
                    "obraId", 2,
                    "titulo", "Obra similar 2",
                    "similitud", 72.3,
                    "dimensionesSimilares", List.of("Técnica")
                )
            ),
            "resumen", Map.of(
                "totalObrasEnColeccion", 150,
                "obrasSimilaresEncontradas", 12,
                "estiloPredominante", "Impresionismo",
                "periodoComun", "Finales del siglo XIX"
            )
        )));
    }
}
