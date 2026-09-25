package com.unla.museo.graphql;

import java.time.LocalDateTime;
import java.util.List;

public class EventoGraphQL {
	private Long id;
	private String titulo;
	private String tipo;
	private LocalDateTime fechaHora;
	private List<String> inscriptos;

	public EventoGraphQL(Long id, String titulo, String tipo, LocalDateTime fechaHora, List<String> inscriptos) {
		this.id = id;
		this.titulo = titulo;
		this.tipo = tipo;
		this.fechaHora = fechaHora;
		this.inscriptos = inscriptos;
	}

	public Long getId() {
		return id;
	}

	public String getTitulo() {
		return titulo;
	}

	public String getTipo() {
		return tipo;
	}

	public LocalDateTime getFechaHora() {
		return fechaHora;
	}

	public List<String> getInscriptos() {
		return inscriptos;
	}
}
