package com.unla.museo.reportes.service;

import com.unla.museo.eventos.util.EstadoEvento;
import com.unla.museo.eventos.util.TipoEvento;
import com.unla.museo.reportes.util.AgruparPor;
import com.unla.museo.reportes.dto.ReporteAsistenciaDTO;

public interface ReporteAsistenciaService {

    /**
     * @param desde  "AAAA-MM-DD" o null
     * @param hasta  "AAAA-MM-DD" o null
     */
    ReporteAsistenciaDTO generar(String desde, String hasta, TipoEvento tipo, EstadoEvento estado,
                                 AgruparPor agruparPor);
}
