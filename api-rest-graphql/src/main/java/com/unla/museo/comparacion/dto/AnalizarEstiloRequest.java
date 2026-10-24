package com.unla.museo.comparacion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request para analizar un estilo artístico.
 * Versión específica para REST.
 */
@Data
public class AnalizarEstiloRequest {

    @NotBlank(message = "El estilo es obligatorio")
    private String estilo;

    private Long obraReferenciaId;
}
