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
        Long responsibleUserId) {
}
