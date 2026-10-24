package com.unla.museo.catalogo.dto;

import lombok.Data;

/**
 * Request para listar obras con paginación y ordenamiento.
 * Versión específica para REST.
 */
@Data
public class ListarObrasRequest {

    private Integer pagina = 0;
    private Integer tamano = 10;
    private String ordenarPor = "id";
    private String orden = "ASC";
}
