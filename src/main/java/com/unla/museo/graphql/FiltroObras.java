package com.unla.museo.graphql;

public class FiltroObras {

    private String palabraClave;
    private String epoca;
    private String tecnica;
    private String ubicacion;
    private Boolean enExhibicion;

    public String getPalabraClave() {
        return palabraClave;
    }

    public void setPalabraClave(String palabraClave) {
        this.palabraClave = palabraClave;
    }

    public String getEpoca() {
        return epoca;
    }

    public void setEpoca(String epoca) {
        this.epoca = epoca;
    }

    public String getTecnica() {
        return tecnica;
    }

    public void setTecnica(String tecnica) {
        this.tecnica = tecnica;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public Boolean getEnExhibicion() {
        return enExhibicion;
    }

    public void setEnExhibicion(Boolean enExhibicion) {
        this.enExhibicion = enExhibicion;
    }
}