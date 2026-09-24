package com.unla.museo.graphql;

public class EventoPopular {

    private Long id;
    private String titulo;
    private Integer cantidadInscriptos;

    public EventoPopular(Long id, String titulo, Integer cantidadInscriptos) {
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

    public Integer getCantidadInscriptos() {
        return cantidadInscriptos;
    }
}