package com.unla.museo.graphql;

public class FiltroReporte {
	private String desde, hasta, tipo;
	private EstadoEvento estado;
	private AgruparPor agruparPor;

	public String getDesde() {
		return desde;
	}

	public void setDesde(String v) {
		desde = v;
	}

	public String getHasta() {
		return hasta;
	}

	public void setHasta(String v) {
		hasta = v;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String v) {
		tipo = v;
	}

	public EstadoEvento getEstado() {
		return estado;
	}

	public void setEstado(EstadoEvento v) {
		estado = v;
	}

	public AgruparPor getAgruparPor() {
		return agruparPor;
	}

	public void setAgruparPor(AgruparPor v) {
		agruparPor = v;
	}
}
