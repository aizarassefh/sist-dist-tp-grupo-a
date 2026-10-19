package com.unla.museo.eventos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ModificarCupoRequest(
        @NotNull(message = "El cupo máximo es obligatorio")
        @Min(value = 1, message = "El cupo debe ser de al menos 1")
        Integer cupoMaximo
) {}