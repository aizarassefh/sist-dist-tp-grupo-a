package com.unla.museo.mensajeria;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MensajeSalienteRepository extends JpaRepository<MensajeSalienteEntity, String> {

    List<MensajeSalienteEntity> findTop50ByFechaPublicacionIsNullOrderByFechaCreacionAscIdMensajeAsc();

    long countByFechaPublicacionIsNotNull();

    // Publicados que el consumidor todavía no registró en mensajes_procesados.
    @Query("""
            select m.idMensaje from MensajeSalienteEntity m
            where m.fechaPublicacion is not null
              and not exists (select 1 from MensajeProcesadoEntity p
                              where p.id.consumidor = :consumidor and p.id.idMensaje = m.idMensaje)
            order by m.fechaCreacion
            """)
    List<String> idsPublicadosSinProcesar(@Param("consumidor") String consumidor, Pageable limite);
}
