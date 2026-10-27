package com.unla.museo.usuarios.grpc.mapper;

import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.usuarios.entity.InteraccionUsuario;
import com.unla.museo.usuarios.entity.PreferenciasUsuario;
import com.unla.museo.usuarios.grpc.Interaccion;
import com.unla.museo.usuarios.grpc.Perfil;
import com.unla.museo.usuarios.grpc.Preferencias;

public final class UsuariosGrpcMapper {

    private UsuariosGrpcMapper() {
    }

    public static Perfil aPerfil(UsuarioEntity usuario) {
        return Perfil.newBuilder()
                .setId(usuario.getId())
                .setEmail(usuario.getEmail())
                .setNombre(texto(usuario.getNombre()))
                .setApellido(texto(usuario.getApellido()))
                .setRol(usuario.getRol() != null ? usuario.getRol().getId() : "")
                .setPreferencias(aPreferencias(usuario.getPreferencias()))
                .build();
    }

    public static Preferencias aPreferencias(PreferenciasUsuario preferencias) {
        return Preferencias.newBuilder()
                .addAllMovimientos(preferencias.getMovimientos())
                .addAllTecnicas(preferencias.getTecnicas())
                .addAllTematicas(preferencias.getTematicas())
                .build();
    }

    public static Interaccion aInteraccion(InteraccionUsuario interaccion) {
        return Interaccion.newBuilder()
                .setObraId(interaccion.getObraId())
                .setTipo(interaccion.getTipo().name())
                .setFecha(interaccion.getFecha().toString())
                .build();
    }

    private static String texto(String valor) {
        return valor != null ? valor : "";
    }
}
