package com.unla.museo.dto.request;


import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class UserCreateRequest {

    @Email(message = "El email no tiene un formato válido")
    @NotBlank(message = "El email es obligatorio")
    private String email;

    @NotBlank(message = "El nombre es obligatorio")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    private String lastName;

    private String roleId;

    @NotNull(message = "El número es obligatorio")
    private String phoneNumber;

    @NotNull(message = "El estado del usuario es obligatorio")
    private boolean active;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe estar entre los 8 y 100 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#_-])[A-Za-z\\d@$!%*?&.#_-]{8,}$",
            message = "La contraseña debe contener al menos una(1) letra mayúsculas, una(1) minúscula, un(1) número y un caracter special"
    )
    String password;
}
