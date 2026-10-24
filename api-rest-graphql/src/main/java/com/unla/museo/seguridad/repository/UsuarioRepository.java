package com.unla.museo.seguridad.repository;

import com.unla.museo.seguridad.entity.UsuarioEntity;

import java.util.List;
import java.util.Optional;


public interface UsuarioRepository {

    Optional<UsuarioEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    UsuarioEntity save(UsuarioEntity usuarioEntity);

    Optional<UsuarioEntity> findById(Long id);

    List<UsuarioEntity> findByRolIdOrderByNombreAscApellidoAsc(String roleId);

}
