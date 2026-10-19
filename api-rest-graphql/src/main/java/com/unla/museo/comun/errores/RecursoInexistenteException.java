package com.unla.museo.comun.errores;

/**
 * El recurso pedido no existe (evento, inscripción, filtro favorito, obra...).
 * Se traduce a 404 en GlobalExceptionHandler.
 */
public class RecursoInexistenteException extends RuntimeException {

    public RecursoInexistenteException(String mensaje) {
        super(mensaje);
    }
}
