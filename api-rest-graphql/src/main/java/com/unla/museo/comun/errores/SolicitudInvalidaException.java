package com.unla.museo.comun.errores;

/**
 * Solicitud inválida por una regla de negocio que no es de Bean Validation,
 * por ejemplo un rango de fechas invertido. Se traduce a 400 en
 * GlobalExceptionHandler.
 */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
