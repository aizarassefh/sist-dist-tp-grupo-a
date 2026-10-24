package com.unla.museo.seguridad.dto;


import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class CrearUsuarioRequest {

    @Email(message = "El email no tiene un formato válido")
    @NotBlank(message = "El email es obligatorio")
    private String email;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    private String telefono;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe estar entre los 8 y 100 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#_-])[A-Za-z\\d@$!%*?&.#_-]{8,}$",
            message = "La contraseña debe contener al menos una(1) letra mayúsculas, una(1) minúscula, un(1) número y un caracter special"
    )
    String password;

    // Normalizado (trim + minúsculas) ya en el binding de Jackson, antes de
    // que corra @Email: si no, un email con espacios (típico de un
    // copiar/pegar) rebota con 400 antes de llegar a normalizarse en el service.
    public void setEmail(String email) {
        this.email = email == null ? null : email.strip().toLowerCase();
    }
}
