package com.unla.museo.catalogo.entity;

import com.unla.museo.seguridad.entity.UsuarioEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "COMENTARIOS", indexes = {
        @Index(name = "idx_comentario_obra_id", columnList = "OBRA_ID")
})
public class ComentarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OBRA_ID", nullable = false)
    private ObraEntity obra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    private UsuarioEntity usuario;

    @Column(name = "TEXTO", columnDefinition = "TEXT", nullable = false)
    private String texto;

    @Column(name = "FECHA", nullable = false)
    private LocalDateTime fecha;
}
