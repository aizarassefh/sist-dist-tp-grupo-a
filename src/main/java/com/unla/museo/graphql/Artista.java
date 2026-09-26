package com.unla.museo.graphql;

public class Artista {
	private String nombre;
	private String biografia;

	public Artista(String nombre, String biografia) {
		this.nombre = nombre;
		this.biografia = biografia;
	}

	public String getNombre() {
		return nombre;
	}

	public String getBiografia() {
		return biografia;
	}
}
