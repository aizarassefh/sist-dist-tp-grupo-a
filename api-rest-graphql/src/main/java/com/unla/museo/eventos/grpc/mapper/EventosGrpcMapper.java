package com.unla.museo.eventos.grpc.mapper;

import com.unla.museo.eventos.entity.EventoEntity;
import com.unla.museo.eventos.grpc.Evento;

public final class EventosGrpcMapper {

    private EventosGrpcMapper() {
    }

    // inscriptos viene de un conteo real sobre inscripciones y no de
    // inscripcionesActuales, que los datos de ejemplo no mantienen.
    public static Evento aEvento(EventoEntity evento, int inscriptos) {
        return Evento.newBuilder()
                .setId(evento.getId())
                .setTitulo(evento.getTitulo())
                .setDescripcion(evento.getDescripcion())
                .setTipo(evento.getTipo().name())
                .setFechaHora(evento.getFechaHora().toString())
                .setDuracionMinutos(evento.getDuracionMinutos())
                .setCupoMaximo(evento.getCupoMaximo())
                .setInscriptos(inscriptos)
                .setMovimiento(evento.getMovimiento() != null ? evento.getMovimiento() : "")
                .build();
    }
}
