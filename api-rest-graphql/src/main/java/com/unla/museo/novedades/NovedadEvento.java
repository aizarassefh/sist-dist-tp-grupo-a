package com.unla.museo.novedades;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record NovedadEvento(
        UUID messageId,
        NovedadTipo type,
        Long eventId,
        String title,
        LocalDateTime fechaHora,
        Instant occurredAt,
        Long responsibleUserId,
        String previousData,
        String newData) {

    public NovedadEvento(
            UUID messageId,
            NovedadTipo type,
            Long eventId,
            String title,
            LocalDateTime fechaHora,
            Instant occurredAt,
            Long responsibleUserId) {
        this(messageId, type, eventId, title, fechaHora, occurredAt, responsibleUserId, null, null);
    }
}
