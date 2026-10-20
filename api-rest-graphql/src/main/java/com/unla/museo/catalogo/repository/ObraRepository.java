package com.unla.museo.catalogo.repository;

import com.unla.museo.catalogo.entity.ObraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObraRepository extends JpaRepository<ObraEntity, Long> {

    /**
     * Buscar obras por estilo (contiene ignorando mayúsculas)
     */
    List<ObraEntity> findByEstiloContainingIgnoreCase(String estilo);

    /**
     * Buscar obras por técnica (contiene ignorando mayúsculas)
     */
    List<ObraEntity> findByTecnicaContainingIgnoreCase(String tecnica);

    /**
     * Buscar obras por ID de artista
     */
    List<ObraEntity> findByArtistaId(Long artistaId);

    /**
     * Buscar obras por época
     */
    List<ObraEntity> findByEpoca(String epoca);

    /**
     * Buscar obras por ubicación
     */
    List<ObraEntity> findByUbicacion(String ubicacion);

    /**
     * Buscar obras que están actualmente en exhibición
     */
    List<ObraEntity> findByEnExhibicionTrue();

    /**
     * Buscar obras por estilo y que están en exhibición
     */
    List<ObraEntity> findByEstiloAndEnExhibicion(String estilo, boolean enExhibicion);

    /**
     * Incrementar el contador de visitas de una obra
     */
    @Modifying
    @Query("UPDATE ObraEntity o SET o.visitas = o.visitas + 1 WHERE o.id = :obraId")
    void incrementarVisitas(@Param("obraId") Long obraId);

    /**
     * Actualizar la calificación promedio de una obra
     */
    @Modifying
    @Query("UPDATE ObraEntity o SET o.calificacionPromedio = :nuevaCalificacion, " +
           "o.totalValoraciones = o.totalValoraciones + 1 " +
           "WHERE o.id = :obraId")
    void actualizarCalificacion(@Param("obraId") Long obraId, @Param("nuevaCalificacion") double nuevaCalificacion);

    
@Query("""
        select o from ObraEntity o
        where (:palabraClave is null
               or lower(o.titulo) like concat('%', :palabraClave, '%')
               or lower(o.descripcion) like concat('%', :palabraClave, '%')
               or lower(o.artista.nombre) like concat('%', :palabraClave, '%'))
          and (:epoca is null or lower(o.epoca) like concat('%', :epoca, '%'))
          and (:tecnica is null or lower(o.tecnica) like concat('%', :tecnica, '%'))
          and (:ubicacion is null or lower(o.ubicacion) like concat('%', :ubicacion, '%'))
          and (:enExhibicion is null or o.enExhibicion = :enExhibicion)
        """)
Page<ObraEntity> buscar(
        @Param("palabraClave") String palabraClave,
        @Param("epoca") String epoca,
        @Param("tecnica") String tecnica,
        @Param("ubicacion") String ubicacion,
        @Param("enExhibicion") Boolean enExhibicion,
        Pageable pageable);


    /**
     * Buscar obras mejor calificadas
     */
    List<ObraEntity> findTop10ByOrderByCalificacionPromedioDesc();

    /**
     * Buscar obras más visitadas
     */
    List<ObraEntity> findTop10ByOrderByVisitasDesc();

    /**
     * Buscar obras por rango de años
     */
    List<ObraEntity> findByAnioBetween(int anioInicio, int anioFin);
}
