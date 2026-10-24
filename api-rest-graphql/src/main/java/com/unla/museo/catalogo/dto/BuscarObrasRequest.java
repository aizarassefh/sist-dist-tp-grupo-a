package com.unla.museo.catalogo.dto;

import lombok.Data;

/**
 * Request para buscar obras con filtros.
 * Versión específica para REST.
 */
@Data
public class BuscarObrasRequest {

    private String consulta;
    private String tecnica;
    private String estilo;
    private Long artistaId;
    private Integer pagina = 0;
    private Integer tamano = 10;
}
