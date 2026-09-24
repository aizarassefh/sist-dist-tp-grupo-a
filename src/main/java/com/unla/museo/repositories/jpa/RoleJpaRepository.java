package com.unla.museo.repositories.jpa;


import com.unla.museo.entities.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, String> {
}