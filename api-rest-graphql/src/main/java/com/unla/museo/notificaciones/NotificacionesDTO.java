package com.unla.museo.notificaciones;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record NotificacionesDTO(long noLeidas, List<NotificacionDTO> notificaciones) {

    public record NotificacionDTO(Long id, String tipo, Long idEvento, String tituloEvento, String mensaje,
                                  Instant fechaOperacion, boolean leida) {

        static NotificacionDTO desde(NotificacionEntity n) {
            return new NotificacionDTO(n.getId(), n.getTipo(), n.getIdEvento(), n.getTituloEvento(), n.getMensaje(),
                    n.getFechaOperacion().truncatedTo(ChronoUnit.SECONDS), n.isLeida());
        }
    }
}
