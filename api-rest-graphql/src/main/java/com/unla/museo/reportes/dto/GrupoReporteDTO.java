package com.unla.museo.reportes.dto;

import com.unla.museo.eventos.util.TipoEvento;

import java.util.List;

/** Tipo GraphQL "GrupoReporte": un grupo por mes, por tipo o por ambos. */
public class GrupoReporteDTO {
    private final String mes;
    private final TipoEvento tipo;
    private final int cantidadDeEventos;
    private final int totalInscriptosAcumulados;
    private final double promedioDeAsistencia;
    private final List<EventoPopularDTO> eventosMasPopulares;

    public GrupoReporteDTO(String mes, TipoEvento tipo, int cantidadDeEventos, int totalInscriptosAcumulados,
                            double promedioDeAsistencia, List<EventoPopularDTO> eventosMasPopulares) {
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

    public TipoEvento getTipo() {
        return tipo;
    }

    public int getCantidadDeEventos() {
        return cantidadDeEventos;
    }

    public int getTotalInscriptosAcumulados() {
        return totalInscriptosAcumulados;
    }

    public double getPromedioDeAsistencia() {
        return promedioDeAsistencia;
    }

    public List<EventoPopularDTO> getEventosMasPopulares() {
        return eventosMasPopulares;
    }
}
