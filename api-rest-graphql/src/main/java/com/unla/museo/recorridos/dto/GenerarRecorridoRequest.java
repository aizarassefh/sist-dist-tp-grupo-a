package com.unla.museo.recorridos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request para generar un recorrido personalizado.
 * Versión específica para REST.
 */
@Data
public class GenerarRecorridoRequest {

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long usuarioId;

    private String tipoRecorrido = "PERSONALIZADO";
    private String estilo;
    private Long artistaId;
    private String fechaInicio;
    private String fechaFin;
    private Integer duracionMinutos;
    private Integer cantidadMaximaObras = 10;
    private String nombrePersonalizado;
    private Boolean guardarAutomaticamente = false;
}
