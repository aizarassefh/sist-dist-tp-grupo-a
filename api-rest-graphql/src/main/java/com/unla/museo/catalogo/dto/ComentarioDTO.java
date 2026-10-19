package com.unla.museo.catalogo.dto;

/** "usuario" es nombre y apellido del autor: nunca su email. */
public record ComentarioDTO(String usuario, String texto, String fecha) {
}
