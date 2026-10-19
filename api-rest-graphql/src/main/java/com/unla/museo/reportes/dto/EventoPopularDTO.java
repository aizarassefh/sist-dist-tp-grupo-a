package com.unla.museo.reportes.dto;

/** Tipo GraphQL "EventoPopular": id, título y cantidad de inscriptos. */
public class EventoPopularDTO {
    private final Long id;
    private final String titulo;
    private final int cantidadInscriptos;

    public EventoPopularDTO(Long id, String titulo, int cantidadInscriptos) {
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
