package com.unla.museo.novedades;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record NovedadDTO(UUID messageId, NovedadTipo type, Long eventId, String title,
                         LocalDateTime fechaHora, Instant occurredAt, Long responsibleUserId) {
    public static NovedadDTO desde(NovedadEntity entity) {
        return new NovedadDTO(entity.getMessageId(), entity.getType(), entity.getEventId(), entity.getTitle(),
                entity.getFechaHora(), entity.getOccurredAt(), entity.getResponsibleUserId());
    }
}
