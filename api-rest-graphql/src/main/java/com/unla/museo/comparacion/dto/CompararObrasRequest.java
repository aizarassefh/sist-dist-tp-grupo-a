package com.unla.museo.comparacion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request para comparar dos obras.
 * Versión específica para REST.
 */
@Data
public class CompararObrasRequest {

    @NotNull(message = "El ID de la obra principal es obligatorio")
    private Long obraPrincipalId;

    @NotNull(message = "El ID de la obra secundaria es obligatorio")
    private Long obraSecundariaId;

    private Long usuarioId;
}
