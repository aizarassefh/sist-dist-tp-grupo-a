package com.unla.museo.repositories;



import com.unla.museo.entities.RoleEntity;

import java.util.Optional;

public interface RoleRepository {
    Optional<RoleEntity> findById(String id);
    RoleEntity save(RoleEntity role);
    void deleteById(String id);
}