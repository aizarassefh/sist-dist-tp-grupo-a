package com.unla.museo.eventos.dto;

import com.unla.museo.eventos.util.TipoEvento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/** Mismo cuerpo para crear (POST) y editar (PUT) un evento. */
@Data
public class EventoRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no puede superar los 200 caracteres")
    private String titulo;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @NotNull(message = "El tipo de evento es obligatorio")
    private TipoEvento tipo;

    @NotNull(message = "La fecha y hora son obligatorias")
    private LocalDateTime fechaHora;

    @NotNull(message = "La duración es obligatoria")
    @Positive(message = "La duración debe ser un número positivo")
    private Integer duracionMinutos;

    @NotNull(message = "El cupo máximo es obligatorio")
    @Positive(message = "El cupo máximo debe ser un número positivo")
    private Integer cupoMaximo;

    @NotNull(message = "Debe indicarse un curador responsable")
    private Long curadorId;
}
