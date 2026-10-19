package com.unla.museo.comun.errores;

public interface MensajesError {

    String INTERNAL_ERROR = "Ocurrió un error inesperado. Por favor, contacte al equipo de soporte";
    // Mismo mensaje para email inexistente y contraseña incorrecta: no hay
    // que revelar si el email está registrado.
    String FALLO_AUTENTICACION = "Email o contraseña incorrectos";
    interface Usuario {
        String NO_ENCONRTADO = "Usuario no encontrado";
        String CONFLICTO_EMAIL = "El email ya esta registrado.";

        interface Rol {
            String NO_ENCONRTADO = "El rol no encontrado";
        }
    }
}
