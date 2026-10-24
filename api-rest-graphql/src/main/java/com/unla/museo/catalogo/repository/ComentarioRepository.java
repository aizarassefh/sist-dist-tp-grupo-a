package com.unla.museo.catalogo.repository;

import com.unla.museo.catalogo.entity.ComentarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComentarioRepository extends JpaRepository<ComentarioEntity, Long> {

    // join fetch del usuario: se necesita su nombre y apellido para cada
    // comentarioEntity, y sin el fetch cada uno dispararía su propia consulta al
    // pedir el autor (N+1). Una sola consulta para todo el lote de obras.
    @Query("""
            select c from ComentarioEntity c
            join fetch c.usuario
            where c.obra.id in :obraIds
            order by c.fecha asc, c.id asc
            """)
    List<ComentarioEntity> buscarPorObraIds(@Param("obraIds") List<Long> obraIds);

    // Buscar comentarios por ID de obra
    List<ComentarioEntity> findByObraId(Long obraId);
}
