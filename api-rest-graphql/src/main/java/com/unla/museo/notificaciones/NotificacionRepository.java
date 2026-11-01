package com.unla.museo.notificaciones;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<NotificacionEntity, Long> {

    List<NotificacionEntity> findByIdUsuarioOrderByFechaOperacionDescIdDesc(Long idUsuario, Pageable limite);

    List<NotificacionEntity> findByIdUsuarioAndLeidaFalseOrderByFechaOperacionDescIdDesc(Long idUsuario, Pageable limite);

    long countByIdUsuarioAndLeidaFalse(Long idUsuario);

    // El filtro por usuario va en la propia sentencia: una notificación ajena
    // afecta 0 filas y responde 404, igual que una inexistente.
    @Transactional
    @Modifying
    @Query("update NotificacionEntity n set n.leida = true where n.id = :id and n.idUsuario = :idUsuario")
    int marcarLeida(@Param("id") Long id, @Param("idUsuario") Long idUsuario);

    @Transactional
    @Modifying
    @Query("update NotificacionEntity n set n.leida = true where n.idUsuario = :idUsuario and n.leida = false")
    int marcarTodasLeidas(@Param("idUsuario") Long idUsuario);
}
