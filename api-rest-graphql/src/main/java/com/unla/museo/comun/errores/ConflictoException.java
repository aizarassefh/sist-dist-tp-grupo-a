package com.unla.museo.comun.errores;

/**
 * Conflicto con el estado actual del recurso: sin cupo, inscripción duplicada,
 * evento ya comenzado, cupo menor que la cantidad de inscriptos, etc. Se
 * traduce a 409 en GlobalExceptionHandler.
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
