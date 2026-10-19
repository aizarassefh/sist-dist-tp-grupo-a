package com.unla.museo.eventos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Cuerpo de crear (POST) y editar (PUT) un filtro favorito, sin id. */
@Data
public class FiltroFavoritoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    private FiltrosDTO filtros;
}
