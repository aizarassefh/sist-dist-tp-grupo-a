package com.unla.museo.gateway.error;

import org.springframework.http.HttpStatus;

/**
 * Error esperable del gateway (pedido inválido, servicio caído, recurso
 * inexistente) con el HTTP que le corresponde. El mensaje se muestra tal cual
 * al cliente, así que nunca debe llevar datos internos.
 */
public class ErrorGateway extends RuntimeException {

    private final HttpStatus estado;

    public ErrorGateway(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public static ErrorGateway pedidoInvalido(String mensaje) {
        return new ErrorGateway(HttpStatus.BAD_REQUEST, mensaje);
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
