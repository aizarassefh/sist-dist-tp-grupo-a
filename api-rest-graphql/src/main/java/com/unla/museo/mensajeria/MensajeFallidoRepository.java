package com.unla.museo.mensajeria;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeFallidoRepository extends JpaRepository<MensajeFallidoEntity, Long> {

    long countByEstado(EstadoFallido estado);

    List<MensajeFallidoEntity> findByEstadoOrderByFechaRecepcionDescIdDesc(EstadoFallido estado, Pageable limite);

    List<MensajeFallidoEntity> findAllByOrderByFechaRecepcionDescIdDesc(Pageable limite);

    List<MensajeFallidoEntity> findByIdMensajeAndEstado(String idMensaje, EstadoFallido estado);
}
