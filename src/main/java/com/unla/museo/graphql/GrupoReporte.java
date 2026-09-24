package com.unla.museo.graphql;

import java.util.List;

public class GrupoReporte {

    private String mes;
    private String tipo;
    private Integer cantidadDeEventos;
    private Integer totalInscriptosAcumulados;
    private Double promedioDeAsistencia;
    private List<EventoPopular> eventosMasPopulares;

    public GrupoReporte(
            String mes,
            String tipo,
            Integer cantidadDeEventos,
            Integer totalInscriptosAcumulados,
            Double promedioDeAsistencia,
            List<EventoPopular> eventosMasPopulares) {

        this.mes = mes;
        this.tipo = tipo;
        this.cantidadDeEventos = cantidadDeEventos;
        this.totalInscriptosAcumulados = totalInscriptosAcumulados;
        this.promedioDeAsistencia = promedioDeAsistencia;
        this.eventosMasPopulares = eventosMasPopulares;
    }

    public String getMes() {
        return mes;
    }

    public String getTipo() {
        return tipo;
    }

    public Integer getCantidadDeEventos() {
        return cantidadDeEventos;
    }

    public Integer getTotalInscriptosAcumulados() {
        return totalInscriptosAcumulados;
    }

    public Double getPromedioDeAsistencia() {
        return promedioDeAsistencia;
    }

    public List<EventoPopular> getEventosMasPopulares() {
        return eventosMasPopulares;
    }
}