package com.unla.museo.seguridad.repository;

import com.unla.museo.seguridad.entity.RolEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Qualifier("RolSQLRepositoryImpl")
@RequiredArgsConstructor
public class RolSQLRepositoryImpl implements RolRepository {
    private final RolJpaRepository rolJpaRepository;

    @Override
    public Optional<RolEntity> findById(String id) {
        return rolJpaRepository.findById(id);
    }



    @Override
    public RolEntity save(RolEntity role) {
        return rolJpaRepository.save(role);
    }
}
