package com.unla.museo.reportes.resolver;

import com.unla.museo.reportes.dto.FiltroReporteInput;
import com.unla.museo.reportes.dto.ReporteAsistenciaDTO;
import com.unla.museo.reportes.service.ReporteAsistenciaService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
public class ReporteAsistenciaResolver {

    private final ReporteAsistenciaService reporteAsistenciaService;

    public ReporteAsistenciaResolver(ReporteAsistenciaService reporteAsistenciaService) {
        this.reporteAsistenciaService = reporteAsistenciaService;
    }

    // El campo es nullable en el schema (reportes.graphqls): un VISITANTE que
    // pide reporteAsistencia junto con obras en la misma operación recibe las
    // obras y, acá, un error FORBIDDEN (GraphQlExceptionResolver) con este
    // campo en null, en vez de tirar abajo toda la respuesta.
    @QueryMapping
    @PreAuthorize("hasAnyRole('CURADOR','ADMINISTRADOR')")
    public ReporteAsistenciaDTO reporteAsistencia(@Argument FiltroReporteInput filtro) {
        String desde = filtro != null ? filtro.getDesde() : null;
        String hasta = filtro != null ? filtro.getHasta() : null;
        var tipo = filtro != null ? filtro.getTipo() : null;
        var estado = filtro != null ? filtro.getEstado() : null;
        var agruparPor = filtro != null ? filtro.getAgruparPor() : null;
        return reporteAsistenciaService.generar(desde, hasta, tipo, estado, agruparPor);
    }
}
