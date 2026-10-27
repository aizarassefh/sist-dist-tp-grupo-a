package com.unla.museo.usuarios.repository;

import com.unla.museo.usuarios.entity.InteraccionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InteraccionUsuarioRepository extends JpaRepository<InteraccionUsuario, Long> {

    // El id desempata interacciones con la misma fecha para que el orden sea estable.
    List<InteraccionUsuario> findByUsuarioIdOrderByFechaDescIdDesc(Long usuarioId);

    boolean existsByUsuarioIdAndObraIdAndTipo(Long usuarioId, Long obraId, InteraccionUsuario.TipoInteraccion tipo);
}
