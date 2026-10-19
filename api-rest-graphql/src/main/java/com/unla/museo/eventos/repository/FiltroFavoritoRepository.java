package com.unla.museo.eventos.repository;

import com.unla.museo.eventos.entity.FiltroFavoritoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FiltroFavoritoRepository extends JpaRepository<FiltroFavoritoEntity, Long> {

    List<FiltroFavoritoEntity> findByUsuarioIdOrderByIdAsc(Long usuarioId);

    // Un favorito ajeno tiene que responder 404, igual que uno inexistente:
    // por eso la búsqueda siempre incluye el usuario dueño en la condición.
    Optional<FiltroFavoritoEntity> findByIdAndUsuarioId(Long id, Long usuarioId);
}
