package com.unla.museo.mensajeria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensajeProcesadoRepository extends JpaRepository<MensajeProcesadoEntity, MensajeProcesadoId> {

    @Query("select count(p) from MensajeProcesadoEntity p where p.id.consumidor = :consumidor")
    long contarPorConsumidor(@Param("consumidor") String consumidor);
}
