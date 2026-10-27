package com.unla.museo.catalogo.repository;

import com.unla.museo.catalogo.entity.ObraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface ObraRepository extends JpaRepository<ObraEntity, Long> {

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

    // join fetch del artista: el servicio gRPC filtra y mapea en memoria (la
    // comparación sin acentos no se puede expresar en JPQL portable entre
    // MariaDB y H2) y necesita el artista de cada obra sin una consulta por fila.
    @Query("select o from ObraEntity o join fetch o.artista order by o.id")
    List<ObraEntity> listarConArtista();

    @Query("select o from ObraEntity o join fetch o.artista where o.id in :ids")
    List<ObraEntity> buscarPorIdsConArtista(@Param("ids") List<Long> ids);

    // join fetch del artista: los inicializadores corren fuera de una transacción
    // y completan también los datos del artista de una obra ya existente.
    @Query("select o from ObraEntity o join fetch o.artista where o.titulo = :titulo")
    Optional<ObraEntity> buscarPorTituloConArtista(@Param("titulo") String titulo);
}
