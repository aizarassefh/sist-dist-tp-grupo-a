package com.unla.museo.catalogo.repository;

import com.unla.museo.catalogo.entity.ArtistaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistaRepository extends JpaRepository<ArtistaEntity, Long> {
}
