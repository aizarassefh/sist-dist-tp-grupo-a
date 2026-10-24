package com.unla.museo.eventos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request para obtener un evento por su ID.
 * Usado por el controlador REST.
 */
@Data
public class ObtenerEventoRequest {

    @NotNull(message = "El ID del evento es obligatorio")
    private Long id;
}
