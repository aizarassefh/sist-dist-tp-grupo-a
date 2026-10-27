package com.unla.museo.gateway.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Traduce toda excepción de los controllers a {@link RespuestaError}. */
@RestControllerAdvice
@Slf4j
public class ManejadorErrores {

    @ExceptionHandler(ErrorGateway.class)
    public ResponseEntity<RespuestaError> alErrorGateway(ErrorGateway e) {
        return responder(e.getEstado(), e.getMessage());
    }

    // Cuerpo ilegible, tipo de parámetro incorrecto, etc. El motivo técnico de
    // Spring no se devuelve: puede nombrar clases internas.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<RespuestaError> alErrorDeSpring(ResponseStatusException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == null || status == HttpStatus.BAD_REQUEST) {
            return responder(HttpStatus.BAD_REQUEST, "El cuerpo o los parámetros del pedido no son válidos");
        }
        return responder(status, status.getReasonPhrase());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> alErrorInesperado(Exception e) {
        log.error("Error inesperado en el gateway", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private ResponseEntity<RespuestaError> responder(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(RespuestaError.de(status, mensaje));
    }
}
