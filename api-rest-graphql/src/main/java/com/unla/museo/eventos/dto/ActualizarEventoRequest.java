package com.unla.museo.eventos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request para actualizar un evento existente.
 * Versión específica para REST.
 */
@Data
public class ActualizarEventoRequest {

    @NotNull(message = "El ID del evento es obligatorio")
    private Long id;

    private String titulo;
    private String descripcion;
    private String tipo;
    private String fechaInicio;
    private String fechaFin;
    private String ubicacion;
    private Integer capacidadMaxima;
    private Boolean activo;
}
