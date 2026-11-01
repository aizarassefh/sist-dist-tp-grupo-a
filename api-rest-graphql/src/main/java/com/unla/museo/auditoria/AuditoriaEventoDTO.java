package com.unla.museo.auditoria;

import com.unla.museo.novedades.NovedadTipo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record AuditoriaEventoDTO(
        UUID messageId,
        NovedadTipo type,
        Long eventId,
        String title,
        LocalDateTime fechaHora,
        Instant occurredAt,
        Long responsibleUserId,
        String messageJson) {

    public static AuditoriaEventoDTO desde(AuditoriaEventoEntity entity) {
        return new AuditoriaEventoDTO(
                entity.getMessageId(), entity.getType(), entity.getEventId(), entity.getTitle(),
                entity.getFechaHora(), entity.getOccurredAt(), entity.getResponsibleUserId(),
                entity.getMessageJson());
    }
}
