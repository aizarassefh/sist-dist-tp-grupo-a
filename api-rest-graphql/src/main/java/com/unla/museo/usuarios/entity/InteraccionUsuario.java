package com.unla.museo.usuarios.entity;

import com.unla.museo.seguridad.entity.UsuarioEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Señal de interés de un visitante sobre una obra. obraId es un Long plano y
 * no una relación JPA: la tabla obras es del módulo catálogo y usuarios no
 * debe depender de ella (misma decisión que filtros_favorito.curador_id).
 */
@Entity
// Nombre distinto al de la primera versión de la rama gRPC: aquellas tablas
// tenían otras columnas (un ENUM con otros valores, columnas NOT NULL) y
// ddl-auto=update no las corrige; con un nombre nuevo Hibernate las crea
// bien también en las bases de desarrollo que ya corrieron esa versión.
@Table(name = "interacciones_visitante")
@Getter
@Setter
@NoArgsConstructor
public class InteraccionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    private UsuarioEntity usuario;

    @Column(name = "OBRA_ID", nullable = false)
    private Long obraId;

    @Column(name = "TIPO_INTERACCION", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TipoInteraccion tipo;

    @Column(name = "FECHA_INTERACCION", nullable = false)
    private LocalDateTime fecha;

    public enum TipoInteraccion {
        VISTA,
        INTERES
    }
}
