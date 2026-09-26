package com.unla.museo.graphql;

public class Comentario {
	private String usuario;
	private String texto;
	private String fecha;

	public Comentario(String usuario, String texto, String fecha) {
		this.usuario = usuario;
		this.texto = texto;
		this.fecha = fecha;
	}

	public String getUsuario() {
		return usuario;
	}

	public String getTexto() {
		return texto;
	}

	public String getFecha() {
		return fecha;
	}
}
