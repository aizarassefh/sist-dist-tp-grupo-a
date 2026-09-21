package com.unla.museo.exception;

public interface ErrorMessage {

    String INTERNAL_ERROR = "Ocurrió un error inesperado. Por favor, contacte al equipo de soporte";
    String AUTH_FAILED = "Error al autenticar";
    interface User {
        String NOT_FOUND = "Usuario no encontrado";
        String INTERNAL_ERROR_CREATION = "¡Error al crear el usuario!";
        String INTERNAL_ERROR_UPDATE = "¡Error al actualizar el usuario!";
        String CONFLICT = "El usuario ya existe";
        String CONFLICT_EMAIL = "El email ya esta registrado.";
        String INVALID_CREDENTIALS = "Credenciales inválidas";

        String DELETED = "Este usuario fue eliminado. Por favor, contacte al equipo de soporte";
        interface Role {
            String MANDATORY = "El rol es obligatorio";
            String NOT_FOUND = "El rol no encontrado";
            String FORBIDDEN = "No se puede asignar el rol al usuario";
            String REQUESTER_ROLE_NOT_FOUND = "El usuario solicitante no tiene un rol asignado";
        }
    }
    interface  Email {
        String ACCOUNT_ACTIVATION_EMAIL_FAILED = "Hubo un problema al enviar el correo de activación, notifique al usuario o proceda a removerlo y repetir la acción";
        String ACCOUNT_ACTIVATED_NOTIFICATION = "Hubo un problema al enviar el correo de notifación de cuenta activada";

    }


}