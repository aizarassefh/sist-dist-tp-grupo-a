package com.unla.museo.auditoria;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistorialActividadRepository extends JpaRepository<HistorialActividadEntity, String> {

    @Query("""
            select h from HistorialActividadEntity h
            where (:idEvento is null or h.idEvento = :idEvento)
              and (:tipo is null or h.tipo = :tipo)
            order by h.fechaOperacion desc, h.idMensaje desc
            """)
    List<HistorialActividadEntity> buscar(@Param("idEvento") Long idEvento, @Param("tipo") String tipo, Pageable limite);
}
