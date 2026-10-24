package com.unla.museo.seguridad.repository;

import com.unla.museo.seguridad.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity,Long> {

    Optional<UsuarioEntity> findById(Long id);
    Optional<UsuarioEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    List<UsuarioEntity> findByRolIdOrderByNombreAscApellidoAsc(String rolId);

}
