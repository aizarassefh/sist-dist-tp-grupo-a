package com.unla.museo.eventos.entity;

import com.unla.museo.eventos.util.TipoEvento;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "EVENTOS", indexes = {
        @Index(name = "idx_evento_fecha_hora", columnList = "FECHA_HORA"),
        @Index(name = "idx_evento_curador_id", columnList = "CURADOR_ID")
})
public class EventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "TITULO", nullable = false, length = 200)
    private String titulo;

    @Column(name = "DESCRIPCION", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 20)
    private TipoEvento tipo;

    @Column(name = "FECHA_HORA", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "DURACION_MINUTOS", nullable = false)
    private Integer duracionMinutos;

    @Column(name = "CUPO_MAXIMO", nullable = false)
    private Integer cupoMaximo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CURADOR_ID", nullable = false)
    private UsuarioEntity curador;

    // Movimiento artístico al que se vincula el evento (nullable): lo consume
    // el motor de recomendaciones vía EventosService.
    @Column(name = "MOVIMIENTO", length = 100)
    private String movimiento;

    // Sin uso en el dominio, pero se mantiene: en las bases que corrieron la
    // primera versión de la rama gRPC la columna quedó NOT NULL sin default, y
    // sin este campo los INSERT de eventos (REST, Entrega 1) fallarían.
    @Column(name = "ACTIVO", nullable = false)
    private boolean activo = true;

    // cascade ALL + orphanRemoval: borrar el evento borra sus inscripciones
    // (se eligió en lugar de rechazar el borrado de un evento con inscriptos).
    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InscripcionEntity> inscripciones = new ArrayList<>();
}
