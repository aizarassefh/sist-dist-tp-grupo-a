package com.unla.museo.eventos.dto;

import com.unla.museo.seguridad.entity.UsuarioEntity;

/**
 * Solo id y nombre y apellido: nunca email, teléfono ni password. Se usa
 * para "curadorResponsable" y para cada entrada de "inscriptos".
 */
public record PersonaDTO(Long id, String nombre) {

    public static PersonaDTO desde(UsuarioEntity usuario) {
        return new PersonaDTO(usuario.getId(), usuario.getNombre() + " " + usuario.getApellido());
    }
}
