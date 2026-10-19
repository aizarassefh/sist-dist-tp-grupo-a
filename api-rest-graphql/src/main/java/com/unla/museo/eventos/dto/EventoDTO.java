package com.unla.museo.eventos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.unla.museo.eventos.util.TipoEvento;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Forma única del contrato para listado y detalle. "inscriptos" se omite
 * del JSON (no se manda null) salvo que quien pregunta sea CURADOR o
 * ADMINISTRADOR: el listado nunca lo completa.
 */
public record EventoDTO(
        Long id,
        String titulo,
        String descripcion,
        TipoEvento tipo,
        LocalDateTime fechaHora,
        Integer duracionMinutos,
        Integer cupoMaximo,
        PersonaDTO curadorResponsable,
        long cantidadInscriptos,
        boolean inscripto,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<PersonaDTO> inscriptos
) {
}
