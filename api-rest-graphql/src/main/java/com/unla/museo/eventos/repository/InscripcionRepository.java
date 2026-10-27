package com.unla.museo.eventos.repository;

import com.unla.museo.eventos.entity.EventoEntity;
import com.unla.museo.eventos.entity.InscripcionEntity;
import com.unla.museo.eventos.entity.util.ConteoPorEvento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InscripcionRepository extends JpaRepository<InscripcionEntity, Long> {

    boolean existsByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);

    long countByEventoId(Long eventoId);

    void deleteByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);

    @EntityGraph(attributePaths = "usuario")
    List<InscripcionEntity> findByEventoIdOrderByFechaInscripcionAsc(Long eventoId);

    @Query("""
            select i.evento.id as eventoId, count(i) as cantidad
            from InscripcionEntity i
            where i.evento.id in :eventoIds
            group by i.evento.id
            """)
    List<ConteoPorEvento> contarPorEventos(@Param("eventoIds") List<Long> eventoIds);

    @Query("""
            select i.evento.id
            from InscripcionEntity i
            where i.evento.id in :eventoIds and i.usuario.id = :usuarioId
            """)
    List<Long> buscarEventoIdsInscriptoDeUsuario(@Param("eventoIds") List<Long> eventoIds, @Param("usuarioId") Long usuarioId);

    // Por email y no por id: es lo que identifica al usuario en el gateway (claim sub del JWT).
    @Query("""
            select i.evento from InscripcionEntity i
            where i.usuario.email = :email
            order by i.evento.fechaHora, i.evento.id
            """)
    List<EventoEntity> buscarEventosDeUsuarioPorEmail(@Param("email") String email);
}
