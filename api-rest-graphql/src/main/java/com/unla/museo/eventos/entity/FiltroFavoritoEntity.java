package com.unla.museo.eventos.entity;

import com.unla.museo.eventos.util.EstadoEvento;
import com.unla.museo.eventos.util.TipoEvento;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "FILTROS_FAVORITO", indexes = @Index(name = "idx_filtro_favorito_usuario_id", columnList = "USUARIO_ID"))
public class FiltroFavoritoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    private UsuarioEntity usuario;

    @Column(name = "NOMBRE", nullable = false, length = 150)
    private String nombre;

    @Column(name = "DESCRIPCION", length = 500)
    private String descripcion;

    @Column(name = "DESDE")
    private LocalDate desde;

    @Column(name = "HASTA")
    private LocalDate hasta;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", length = 20)
    private TipoEvento tipo;

    @Column(name = "CURADOR_ID")
    private Long curadorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", length = 20)
    private EstadoEvento estado;
}
