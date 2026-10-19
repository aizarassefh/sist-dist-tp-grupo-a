package com.unla.museo.reportes.dto;

import java.util.List;

/** Tipo GraphQL "ReporteAsistencia": la raíz que devuelve el resolver. */
public class ReporteAsistenciaDTO {
    private final String fechaCorte;
    private final List<GrupoReporteDTO> grupos;

    public ReporteAsistenciaDTO(String fechaCorte, List<GrupoReporteDTO> grupos) {
        this.fechaCorte = fechaCorte;
        this.grupos = grupos;
    }

    public String getFechaCorte() {
        return fechaCorte;
    }

    public List<GrupoReporteDTO> getGrupos() {
        return grupos;
    }
}
