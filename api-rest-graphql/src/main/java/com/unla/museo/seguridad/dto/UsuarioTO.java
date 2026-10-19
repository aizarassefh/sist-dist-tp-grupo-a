package com.unla.museo.seguridad.dto;

import lombok.Data;

@Data
public class UsuarioTO {
    private Long id;

    private String email;

    private String nombre;

    private String apellido;

    private String rol;

    private String telefono;

    private boolean activo;



}
