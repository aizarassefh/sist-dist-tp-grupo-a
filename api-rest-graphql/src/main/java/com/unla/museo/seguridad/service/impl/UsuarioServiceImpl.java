package com.unla.museo.seguridad.service.impl;


import com.unla.museo.comun.errores.RecursoInexistenteException;
import com.unla.museo.seguridad.dto.CrearUsuarioRequest;
import com.unla.museo.seguridad.dto.PersonaTO;
import com.unla.museo.seguridad.dto.UsuarioTO;
import com.unla.museo.seguridad.dto.mapper.UsuarioMapper;
import com.unla.museo.seguridad.entity.RolEntity;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.comun.errores.MensajesError;
import com.unla.museo.seguridad.exception.UsuarioExistenteException;
import com.unla.museo.seguridad.exception.UsuarioNoEncontradoException;
import com.unla.museo.seguridad.repository.RolRepository;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import com.unla.museo.seguridad.service.UsuarioService;
import com.unla.museo.seguridad.util.Roles;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Qualifier("UserServiceImpl")
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    public UsuarioServiceImpl(
            @Qualifier("UsuarioSQLRepositoryImpl") UsuarioRepository usuarioRepository,
            @Qualifier("RolSQLRepositoryImpl") RolRepository rolRepository,
           PasswordEncoder passwordEncoder, UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioMapper = usuarioMapper;
    }

    @Override
    public UsuarioTO create(CrearUsuarioRequest request) {
        // Normalizado acá (trim + minúsculas) para que el mismo email con
        // mayúsculas o espacios no cree una cuenta duplicada.
        request.setEmail(normalizarEmail(request.getEmail()));
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new UsuarioExistenteException(MensajesError.Usuario.CONFLICTO_EMAIL);
        }
        RolEntity rolEntity = rolRepository.findById(Roles.VISITANTE)
                .orElseThrow(() -> new RecursoInexistenteException(MensajesError.Usuario.Rol.NO_ENCONRTADO));
        UsuarioEntity user = usuarioMapper.toEntity(request, rolEntity,request.getEmail());
        user.setPassword(this.passwordEncoder.encode(request.getPassword()));
        user.setCreacion(LocalDateTime.now());
        user = usuarioRepository.save(user);
        return usuarioMapper.toResponse(user);
    }


    @Override
    public boolean validateCredentials(String email, String rawPassword) {
        String emailNormalizado = normalizarEmail(email);
        // Mismo mensaje genérico si el email no existe o si la contraseña no
        // coincide: no hay que revelar si el email está registrado.
        boolean credencialesValidas = usuarioRepository.findByEmail(emailNormalizado)
                .map(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .orElse(false);
        if (!credencialesValidas) {
            throw new BadCredentialsException(MensajesError.FALLO_AUTENTICACION);
        }
        return true;
    }

    private static String normalizarEmail(String email) {
        return email == null ? null : email.strip().toLowerCase();
    }

    public UsuarioTO getByEmail(String userEmail) {
        UsuarioEntity user = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsuarioNoEncontradoException(MensajesError.Usuario.NO_ENCONRTADO));
        return usuarioMapper.toResponse(user);
    }

    @Override
    public List<PersonaTO> getCuradores() {
        return usuarioRepository.findByRolIdOrderByNombreAscApellidoAsc(Roles.CURADOR).stream()
                .map(usuario -> new PersonaTO(usuario.getId(), usuario.getNombre() + " " + usuario.getApellido()))
                .toList();
    }

}
