package com.unla.museo.auditoria;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEventoEntity, UUID> {
    List<AuditoriaEventoEntity> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
