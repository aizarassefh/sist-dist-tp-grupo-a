package com.unla.museo.reportes.dto;

import com.unla.museo.eventos.util.EstadoEvento;
import com.unla.museo.eventos.util.TipoEvento;
import com.unla.museo.reportes.util.AgruparPor;

/**
 * Argumento GraphQL "FiltroReporte". Clase con setters (no record) a
 * propósito: es el mismo estilo que ya usan los demás input de graphql/ en
 * este proyecto para el binding de argumentos.
 */
public class FiltroReporteInput {
    private String desde;
    private String hasta;
    private TipoEvento tipo;
    private EstadoEvento estado;
    private AgruparPor agruparPor;

    public String getDesde() {
        return desde;
    }

    public void setDesde(String desde) {
        this.desde = desde;
    }

    public String getHasta() {
        return hasta;
    }

    public void setHasta(String hasta) {
        this.hasta = hasta;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public void setTipo(TipoEvento tipo) {
        this.tipo = tipo;
    }

    public EstadoEvento getEstado() {
        return estado;
    }

    public void setEstado(EstadoEvento estado) {
        this.estado = estado;
    }

    public AgruparPor getAgruparPor() {
        return agruparPor;
    }

    public void setAgruparPor(AgruparPor agruparPor) {
        this.agruparPor = agruparPor;
    }
}
