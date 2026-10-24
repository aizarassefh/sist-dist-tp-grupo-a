package com.unla.museo.eventos.dto;

import lombok.Data;

/**
 * Request para listar eventos con filtros opcionales.
 * Usado por el controlador REST.
 */
@Data
public class ListarEventosRequest {

    private String tipo;
    private String fechaDesde;
    private String fechaHasta;
    private Boolean soloActivos = true;
    private Integer pagina = 0;
    private Integer tamano = 10;
}
