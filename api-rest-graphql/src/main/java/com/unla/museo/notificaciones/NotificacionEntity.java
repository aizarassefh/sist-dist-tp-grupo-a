package com.unla.museo.notificaciones;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// id_usuario e id_evento sin clave foránea: son ids de tablas de otros módulos
// y el evento cancelado ya no existe.
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "notificaciones",
        uniqueConstraints = @UniqueConstraint(name = "uk_notificacion_mensaje_usuario", columnNames = {"id_mensaje", "id_usuario"}),
        indexes = @Index(name = "idx_notificacion_usuario", columnList = "id_usuario"))
public class NotificacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_mensaje", nullable = false, length = 36)
    private String idMensaje;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "id_evento", nullable = false)
    private Long idEvento;

    @Column(name = "titulo_evento", nullable = false, length = 200)
    private String tituloEvento;

    @Column(name = "mensaje", nullable = false, length = 500)
    private String mensaje;

    @Column(name = "fecha_operacion", nullable = false)
    private Instant fechaOperacion;

    @Column(name = "leida", nullable = false)
    private boolean leida;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;
}
