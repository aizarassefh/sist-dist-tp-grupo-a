package com.unla.museo.seguridad.service;

import com.unla.museo.comun.errores.SolicitudInvalidaException;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.comun.errores.MensajesError;
import com.unla.museo.seguridad.exception.UsuarioNoEncontradoException;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import com.unla.museo.seguridad.util.Roles;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class UsuarioActualServiceImpl implements UsuarioActualService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioActualServiceImpl(@Qualifier("UsuarioSQLRepositoryImpl") UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UsuarioEntity obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException(MensajesError.Usuario.NO_ENCONRTADO));
    }

    @Override
    public boolean esGestor(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(autoridad -> autoridad.equals("ROLE_" + Roles.CURADOR) || autoridad.equals("ROLE_" + Roles.ADMIN));
    }

    @Override
    public UsuarioEntity obtenerCuradorValido(Long curadorId) {
        if (curadorId == null) {
            throw new SolicitudInvalidaException("Debe indicarse un curador responsable");
        }
        UsuarioEntity usuario = usuarioRepository.findById(curadorId)
                .orElseThrow(() -> new SolicitudInvalidaException("El curador indicado no existe"));
        if (usuario.getRol() == null || !Roles.CURADOR.equals(usuario.getRol().getId())) {
            throw new SolicitudInvalidaException("El usuario indicado no tiene rol de curador");
        }
        return usuario;
    }
}
