package com.unla.museo.seguridad.repository;

import com.unla.museo.seguridad.entity.UsuarioEntity;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Qualifier("UsuarioSQLRepositoryImpl")
@Repository
@AllArgsConstructor
public class UsuarioSQLRepositoryImpl implements UsuarioRepository {
    private final UsuarioJpaRepository usuarioJpaRepository;

    @Override
    public Optional<UsuarioEntity> findByEmail(String email) {
        return usuarioJpaRepository.findByEmail(email);
    }
    @Override
    public boolean existsByEmail(String email) {
        return usuarioJpaRepository.existsByEmail(email);
    }

    @Override
    public UsuarioEntity save(UsuarioEntity usuarioEntity) {
        return usuarioJpaRepository.save(usuarioEntity);
    }


    @Override
    public Optional<UsuarioEntity> findById(Long id) {
        return this.usuarioJpaRepository.findById(id);
    }

    @Override
    public List<UsuarioEntity> findByRolIdOrderByNombreAscApellidoAsc(String roleId) {
        return this.usuarioJpaRepository.findByRolIdOrderByNombreAscApellidoAsc(roleId);
    }


}
