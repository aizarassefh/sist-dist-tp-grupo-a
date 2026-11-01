package com.unla.museo.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

// La clave primaria es el idMensaje: un mismo acontecimiento nunca se registra dos veces.
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "historial_actividad", indexes = @Index(name = "idx_historial_evento", columnList = "id_evento"))
public class HistorialActividadEntity {

    @Id
    @Column(name = "id_mensaje", length = 36)
    private String idMensaje;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "id_evento", nullable = false)
    private Long idEvento;

    @Column(name = "titulo_evento", nullable = false, length = 200)
    private String tituloEvento;

    @Column(name = "fecha_hora_evento", nullable = false)
    private LocalDateTime fechaHoraEvento;

    @Column(name = "fecha_operacion", nullable = false)
    private Instant fechaOperacion;

    @Column(name = "id_usuario_responsable")
    private Long idUsuarioResponsable;

    @Column(name = "nombre_usuario_responsable", length = 200)
    private String nombreUsuarioResponsable;

    // Nombres separados por coma; vacío en ALTA y CANCELACION.
    @Column(name = "campos_modificados", length = 500)
    private String camposModificados;

    @Column(name = "datos_anteriores", columnDefinition = "TEXT")
    private String datosAnteriores;

    @Column(name = "datos_posteriores", columnDefinition = "TEXT")
    private String datosPosteriores;

    @Column(name = "fecha_procesamiento", nullable = false)
    private Instant fechaProcesamiento;
}
