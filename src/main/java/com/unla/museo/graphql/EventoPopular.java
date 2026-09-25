package com.unla.museo.graphql;

public class EventoPopular {
	private Long id;
	private String titulo;
	private int cantidadInscriptos;

	public EventoPopular(Long id, String titulo, int cantidadInscriptos) {
		this.id = id;
		this.titulo = titulo;
		this.cantidadInscriptos = cantidadInscriptos;
	}

	public Long getId() {
		return id;
	}

	public String getTitulo() {
		return titulo;
	}

	public int getCantidadInscriptos() {
		return cantidadInscriptos;
	}
}
