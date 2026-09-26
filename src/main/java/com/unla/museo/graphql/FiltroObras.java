package com.unla.museo.graphql;

public class FiltroObras {
	private String palabraClave, epoca, tecnica, ubicacion;
	private Boolean enExhibicion;

	public String getPalabraClave() {
		return palabraClave;
	}

	public void setPalabraClave(String v) {
		palabraClave = v;
	}

	public String getEpoca() {
		return epoca;
	}

	public void setEpoca(String v) {
		epoca = v;
	}

	public String getTecnica() {
		return tecnica;
	}

	public void setTecnica(String v) {
		tecnica = v;
	}

	public String getUbicacion() {
		return ubicacion;
	}

	public void setUbicacion(String v) {
		ubicacion = v;
	}

	public Boolean getEnExhibicion() {
		return enExhibicion;
	}

	public void setEnExhibicion(Boolean v) {
		enExhibicion = v;
	}
}
