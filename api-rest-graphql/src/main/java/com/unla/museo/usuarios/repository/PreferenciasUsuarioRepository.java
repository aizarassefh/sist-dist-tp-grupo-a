package com.unla.museo.usuarios.repository;

import com.unla.museo.usuarios.entity.PreferenciasUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la gestión de preferencias de usuario.
 */
@Repository
public interface PreferenciasUsuarioRepository extends JpaRepository<PreferenciasUsuario, Long> {
}
