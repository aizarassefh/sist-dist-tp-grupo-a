package com.unla.museo.seguridad.repository;

import com.unla.museo.seguridad.entity.RolEntity;

import java.util.Optional;

public interface RolRepository {
    Optional<RolEntity> findById(String id);
    RolEntity save(RolEntity role);
}
