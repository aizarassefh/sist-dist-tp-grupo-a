package com.unla.museo.eventos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request para cancelar una inscripción a un evento.
 * Versión específica para REST.
 */
@Data
public class CancelarInscripcionRequest {

    @NotNull(message = "El ID de la inscripción es obligatorio")
    private Long inscripcionId;

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long usuarioId;
}
