package com.unla.museo.recomendaciones.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Request para obtener recomendaciones de obras para un usuario.
 * Versión específica para REST.
 */
@Data
public class ObtenerRecomendacionesRequest {

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long usuarioId;

    private Integer cantidad = 10;
    private List<String> excluirObrasIds = new ArrayList<>();
    private String contexto = "INICIO";
}
