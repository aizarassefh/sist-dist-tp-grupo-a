package com.unla.museo.graphql;

public class FiltroReporte {

    private String desde;
    private String hasta;
    private String tipo;
    private String estado;
    private String agruparPor;

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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getAgruparPor() {
        return agruparPor;
    }

    public void setAgruparPor(String agruparPor) {
        this.agruparPor = agruparPor;
    }
}