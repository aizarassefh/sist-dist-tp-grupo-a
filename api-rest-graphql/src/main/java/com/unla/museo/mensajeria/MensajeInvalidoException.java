package com.unla.museo.mensajeria;

/**
 * El mensaje no se puede interpretar. Reintentar no lo arregla, por eso el
 * manejador de errores de los consumidores lo manda directo al DLT.
 */
public class MensajeInvalidoException extends RuntimeException {

    public MensajeInvalidoException(String mensaje) {
        super(mensaje);
    }

    public MensajeInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
