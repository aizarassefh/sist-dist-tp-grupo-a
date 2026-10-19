package com.unla.museo.seguridad.repository;


import com.unla.museo.seguridad.entity.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolJpaRepository extends JpaRepository<RolEntity, String> {
}
