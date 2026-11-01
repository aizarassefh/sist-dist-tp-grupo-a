package com.unla.museo.auditoria;

import com.unla.museo.novedades.NovedadEvento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/** Registro persistente de una novedad procesada desde Kafka. */
@Entity
@Table(name = "HISTORIAL_EVENTOS")
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaEventoEntity {
    @Id
    @Column(name = "MESSAGE_ID", nullable = false, updatable = false)
    private UUID messageId;


    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_OPERACION", nullable = false, length = 20)
    private com.unla.museo.novedades.NovedadTipo type;

    @Column(name = "EVENTO_ID", nullable = false)
    private Long eventId;

    @Column(name = "TITULO_EVENTO", nullable = false, length = 200)
    private String title;

    @Column(name = "FECHA_HORA_EVENTO")
    private LocalDateTime fechaHora;

    @Column(name = "FECHA_OPERACION", nullable = false)
    private Instant occurredAt;

    @Column(name = "USUARIO_RESPONSABLE_ID")
    private Long responsibleUserId;

    /** JSON original del mensaje, para conservar los datos procesados. */
    @Column(name = "MENSAJE_JSON", nullable = false, columnDefinition = "TEXT")
    private String messageJson;


    public AuditoriaEventoEntity(NovedadEvento event, String messageJson) {
        this.messageId = event.messageId();
        this.type = event.type();
        this.eventId = event.eventId();
        this.title = event.title();
        this.fechaHora = event.fechaHora();
        this.occurredAt = event.occurredAt();
        this.responsibleUserId = event.responsibleUserId();
        this.messageJson = messageJson;
    }
}
