package com.unla.museo.catalogo.repository;

import com.unla.museo.catalogo.entity.ArtistaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistaRepository extends JpaRepository<ArtistaEntity, Long> {

    // Una obra nueva reutiliza al artista si ya existe (p. ej. dos obras de Van Gogh).
    Optional<ArtistaEntity> findFirstByNombre(String nombre);
}
