package com.unla.museo.eventos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request para inscribir un usuario en un evento.
 * Versión específica para REST.
 */
@Data
public class InscribirUsuarioRequest {

    @NotNull(message = "El ID del evento es obligatorio")
    private Long eventoId;

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long usuarioId;
}
