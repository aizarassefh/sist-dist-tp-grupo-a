package com.unla.museo.graphql;

import java.util.List;

public class ReporteAsistencia {

    private String fechaCorte;
    private List<GrupoReporte> grupos;

    public ReporteAsistencia(
            String fechaCorte,
            List<GrupoReporte> grupos) {

        this.fechaCorte = fechaCorte;
        this.grupos = grupos;
    }

    public String getFechaCorte() {
        return fechaCorte;
    }

    public List<GrupoReporte> getGrupos() {
        return grupos;
    }
}