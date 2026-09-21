package com.unla.museo.repositories.impl;

import com.unla.museo.entities.RoleEntity;
import com.unla.museo.repositories.RoleRepository;
import com.unla.museo.repositories.jpa.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Qualifier("RoleSQLRepositoryImpl")
@RequiredArgsConstructor
public class RoleSQLRepositoryImpl implements RoleRepository {
    private final RoleJpaRepository roleJpaRepository;

    @Override
    public Optional<RoleEntity> findById(String id) {
        return roleJpaRepository.findById(id);
    }



    @Override
    public RoleEntity save(RoleEntity role) {
        return roleJpaRepository.save(role);
    }

    @Override
    public void deleteById(String id) {
        roleJpaRepository.deleteById(id);
    }
}
