package com.unla.museo.comparacion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request para obtener obras similares a una obra de referencia.
 * Versión específica para REST.
 */
@Data
public class ObtenerObrasSimilaresRequest {

    @NotNull(message = "El ID de la obra es obligatorio")
    private Long obraId;

    private String criterio = "ESTILO";
    private Integer cantidad = 5;
}
