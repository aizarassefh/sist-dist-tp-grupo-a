package com.unla.museo.eventos.entity;

import com.unla.museo.seguridad.entity.UsuarioEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "INSCRIPCIONES",
        uniqueConstraints = @UniqueConstraint(name = "uk_inscripcion_evento_usuario", columnNames = {"EVENTO_ID", "USUARIO_ID"}),
        indexes = @Index(name = "idx_inscripcion_usuario_id", columnList = "USUARIO_ID"))
public class InscripcionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "EVENTO_ID", nullable = false)
    private EventoEntity evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    private UsuarioEntity usuario;

    @Column(name = "FECHA_INSCRIPCION", nullable = false)
    private LocalDateTime fechaInscripcion;

    @Column(name = "ESTADO", length = 50)
    @Enumerated(EnumType.STRING)
    private EstadoInscripcion estado = EstadoInscripcion.INSCRIPTO;

    @Column(name = "ASISTENCIA")
    private Boolean asistencia = false;

    /**
     * Estados posibles de una inscripción
     */
    public enum EstadoInscripcion {
        INSCRIPTO,
        CANCELADO,
        ASISTIO,
        NO_PRESENTADO
    }

    public EstadoInscripcion getEstado() {
        return estado != null ? estado : EstadoInscripcion.INSCRIPTO;
    }

    public void setEstado(EstadoInscripcion estado) {
        this.estado = estado;
    }

    public boolean isAsistencia() {
        return asistencia != null ? asistencia : false;
    }

    public void setAsistencia(Boolean asistencia) {
        this.asistencia = asistencia;
    }
}
