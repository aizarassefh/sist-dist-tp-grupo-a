package com.unla.museo.eventos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request para generar un reporte de asistencia a un evento.
 * Versión específica para REST.
 */
@Data
public class ReporteRequest {

    @NotNull(message = "El ID del evento es obligatorio")
    private Long eventoId;
}
