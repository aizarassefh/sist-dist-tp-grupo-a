package com.unla.museo.usuarios.repository;

import com.unla.museo.usuarios.entity.InteraccionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para la gestión de interacciones de usuarios.
 * Proporciona métodos para consultar el historial de visitas y actividades.
 */
@Repository
public interface InteraccionUsuarioRepository extends JpaRepository<InteraccionUsuario, Long> {

    /**
     * Busca todas las interacciones de un usuario específico
     */
    List<InteraccionUsuario> findByUsuarioIdOrderByFechaInteraccionDesc(Long usuarioId);

    /**
     * Busca las interacciones de un usuario en un rango de fechas
     */
    @Query("SELECT i FROM InteraccionUsuario i WHERE i.usuario.id = :usuarioId " +
           "AND i.fechaInteraccion >= :desde ORDER BY i.fechaInteraccion DESC")
    List<InteraccionUsuario> findByUsuarioIdDesde(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDateTime desde);

    /**
     * Busca las interacciones de un usuario por tipo
     */
    List<InteraccionUsuario> findByUsuarioIdAndTipoInteraccion(
            Long usuarioId, InteraccionUsuario.TipoInteraccion tipoInteraccion);

    /**
     * Busca las obras favoritas de un usuario
     */
    @Query("SELECT DISTINCT i.obra.id FROM InteraccionUsuario i " +
           "WHERE i.usuario.id = :usuarioId AND i.tipoInteraccion = 'FAVORITO'")
    List<Long> findObrasFavoritasByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Cuenta las interacciones de un usuario por tipo
     */
    long countByUsuarioIdAndTipoInteraccion(
            Long usuarioId, InteraccionUsuario.TipoInteraccion tipoInteraccion);

    /**
     * Busca interacciones recientes de todos los usuarios (para análisis colaborativo)
     */
    @Query("SELECT i FROM InteraccionUsuario i WHERE i.fechaInteraccion >= :desde " +
           "ORDER BY i.fechaInteraccion DESC")
    List<InteraccionUsuario> findInteraccionesRecientes(@Param("desde") LocalDateTime desde);

    /**
     * Busca usuarios similares basados en patrones de visita
     */
    @Query("SELECT DISTINCT i2.usuario.id FROM InteraccionUsuario i1 " +
           "JOIN InteraccionUsuario i2 ON i1.obra.id = i2.obra.id " +
           "WHERE i1.usuario.id = :usuarioId AND i1.tipoInteraccion = 'VISITA' " +
           "AND i2.usuario.id != :usuarioId AND i2.tipoInteraccion = 'VISITA'")
    List<Long> findUsuariosSimilaresPorVisitas(@Param("usuarioId") Long usuarioId);
}