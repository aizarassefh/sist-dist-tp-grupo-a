package com.unla.museo.mensajeria;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado completo del evento en un momento dado. Los inscriptos viajan en el
 * mensaje porque la cancelación los borra junto con el evento: sin ellos el
 * consumidor de notificaciones tendría que leer tablas del módulo de eventos.
 * El tipo es texto (no el enum de eventos) para que los consumidores no
 * dependan de ese módulo.
 */
public record EventoFoto(
        Long id,
        String titulo,
        String descripcion,
        String tipo,
        LocalDateTime fechaHora,
        Integer duracionMinutos,
        Integer cupoMaximo,
        String movimiento,
        Long idCurador,
        String nombreCurador,
        List<Long> idsInscriptos
) {
}
