package com.unla.museo.comun.errores;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cuerpo uniforme de error de la API REST. Nunca lleva stack traces ni
 * nombres de clases: eso queda solo en el log del servidor (ver
 * GlobalExceptionHandler). "detalles" solo se completa en errores de
 * validación, campo por campo.
 */
public record RespuestaError(
        int codigo,
        String estado,
        String mensaje,
        @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, String> detalles,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss") LocalDateTime fechaHora
) {
    public RespuestaError(int codigo, String estado, String mensaje, Map<String, String> detalles) {
        this(codigo, estado, mensaje, detalles, LocalDateTime.now());
    }
}
