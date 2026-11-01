package com.unla.museo.novedades;

import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
public class NovedadSubscriptionResolver {
    private final NovedadStream stream;

    public NovedadSubscriptionResolver(NovedadStream stream) {
        this.stream = stream;
    }

    @SubscriptionMapping
    public Flux<NovedadDTO> novedades() {
        return stream.flujo();
    }
}
