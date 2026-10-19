package com.unla.museo.eventos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Request para crear un nuevo evento.
 * Versión específica para REST (separada del gRPC).
 */
@Data
public class CrearEventoRequest {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String descripcion;

    @NotBlank(message = "El tipo de evento es obligatorio")
    private String tipo;

    @NotBlank(message = "La fecha de inicio es obligatoria")
    private String fechaInicio;

    private String fechaFin;

    private String ubicacion;

    @NotNull(message = "La capacidad máxima es obligatoria")
    @Positive(message = "La capacidad debe ser positiva")
    private Integer capacidadMaxima;

    private Long creadoPor;
}
