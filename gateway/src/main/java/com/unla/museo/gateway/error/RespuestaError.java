package com.unla.museo.gateway.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Cuerpo de error idéntico al de api-rest-graphql (mismas claves y mismo
 * formato de fecha) para que el front trate igual los errores de ambos
 * backends. Nunca lleva stack traces.
 */
public record RespuestaError(
        int codigo,
        String estado,
        String mensaje,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss") LocalDateTime fechaHora
) {
    public static RespuestaError de(HttpStatus status, String mensaje) {
        return new RespuestaError(status.value(), status.name(), mensaje, LocalDateTime.now());
    }
}
