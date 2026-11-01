package com.unla.museo.mensajeria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Outbox: la novedad se guarda en la misma transacción que la operación del evento. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "mensajes_salientes", indexes = @Index(name = "idx_saliente_pendientes", columnList = "fecha_publicacion, fecha_creacion"))
public class MensajeSalienteEntity {

    @Id
    @Column(name = "id_mensaje", length = 36)
    private String idMensaje;

    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    @Column(name = "clave", nullable = false, length = 100)
    private String clave;

    @Column(name = "contenido", nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    // null = pendiente de publicar.
    @Column(name = "fecha_publicacion")
    private Instant fechaPublicacion;

    @Column(name = "intentos", nullable = false)
    private int intentos;

    @Column(name = "ultimo_error", length = 1000)
    private String ultimoError;
}
