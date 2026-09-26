package com.unla.museo.graphql;

import java.util.List;

public class GrupoReporte {
	private String mes, tipo;
	private int cantidadDeEventos, totalInscriptosAcumulados;
	private double promedioDeAsistencia;
	private List<EventoPopular> eventosMasPopulares;

	public GrupoReporte(String mes, String tipo, int cantidadDeEventos, int totalInscriptosAcumulados,
			double promedioDeAsistencia, List<EventoPopular> eventosMasPopulares) {
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

	public int getCantidadDeEventos() {
		return cantidadDeEventos;
	}

	public int getTotalInscriptosAcumulados() {
		return totalInscriptosAcumulados;
	}

	public double getPromedioDeAsistencia() {
		return promedioDeAsistencia;
	}

	public List<EventoPopular> getEventosMasPopulares() {
		return eventosMasPopulares;
	}
}
