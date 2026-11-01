package com.unla.museo.mensajeria;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record FallidoDTO(
        Long id,
        String idMensaje,
        String consumidor,
        String error,
        String contenido,
        Instant fechaRecepcion,
        EstadoFallido estado
) {

    public static FallidoDTO desde(MensajeFallidoEntity f) {
        return new FallidoDTO(f.getId(), f.getIdMensaje(), f.getConsumidor(), f.getError(), f.getContenido(),
                f.getFechaRecepcion().truncatedTo(ChronoUnit.SECONDS), f.getEstado());
    }
}
