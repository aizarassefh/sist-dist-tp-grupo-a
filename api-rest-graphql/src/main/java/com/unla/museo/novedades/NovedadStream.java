package com.unla.museo.novedades;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/** Puente entre el consumidor Kafka y las suscripciones GraphQL activas. */
@Component
public class NovedadStream {
    private final Sinks.Many<NovedadDTO> novedades = Sinks.many().multicast().directBestEffort();

    public Flux<NovedadDTO> flujo() {
        return novedades.asFlux();
    }

    public void publicar(NovedadEntity novedad) {
        novedades.tryEmitNext(NovedadDTO.desde(novedad));
    }
}
