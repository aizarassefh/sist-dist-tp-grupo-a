package com.unla.museo.novedades;

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

@Entity
@Table(name = "NOVEDADES")
@Getter
@Setter
@NoArgsConstructor
public class NovedadEntity {
    @Id
    @Column(name = "MESSAGE_ID", nullable = false, updatable = false)
    private UUID messageId;
    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 20)
    private NovedadTipo type;
    @Column(name = "EVENTO_ID", nullable = false)
    private Long eventId;
    @Column(name = "TITULO", nullable = false, length = 200)
    private String title;
    @Column(name = "FECHA_HORA", nullable = false)
    private LocalDateTime fechaHora;
    @Column(name = "OCURRED_AT", nullable = false)
    private Instant occurredAt;
    @Column(name = "RESPONSABLE_ID", nullable = false)
    private Long responsibleUserId;

    public NovedadEntity(NovedadEvento event) {
        this.messageId = event.messageId();
        this.type = event.type();
        this.eventId = event.eventId();
        this.title = event.title();
        this.fechaHora = event.fechaHora();
        this.occurredAt = event.occurredAt();
        this.responsibleUserId = event.responsibleUserId();
    }
}
