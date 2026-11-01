package com.unla.museo.auditoria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

public record HistorialActividadDTO(
        String idMensaje,
        String tipo,
        Long idEvento,
        String tituloEvento,
        LocalDateTime fechaHoraEvento,
        Instant fechaOperacion,
        UsuarioResponsableDTO usuarioResponsable,
        List<String> camposModificados,
        JsonNode datosAnteriores,
        JsonNode datosPosteriores,
        Instant fechaProcesamiento
) {

    public record UsuarioResponsableDTO(Long id, String nombre) {
    }

    static HistorialActividadDTO desde(HistorialActividadEntity h, ObjectMapper objectMapper) {
        UsuarioResponsableDTO responsable = h.getIdUsuarioResponsable() != null
                ? new UsuarioResponsableDTO(h.getIdUsuarioResponsable(), h.getNombreUsuarioResponsable())
                : null;
        List<String> campos = h.getCamposModificados() == null || h.getCamposModificados().isBlank()
                ? List.of()
                : Arrays.asList(h.getCamposModificados().split(","));
        return new HistorialActividadDTO(h.getIdMensaje(), h.getTipo(), h.getIdEvento(), h.getTituloEvento(),
                h.getFechaHoraEvento(), h.getFechaOperacion().truncatedTo(ChronoUnit.SECONDS), responsable, campos,
                json(h.getDatosAnteriores(), objectMapper), json(h.getDatosPosteriores(), objectMapper),
                h.getFechaProcesamiento().truncatedTo(ChronoUnit.SECONDS));
    }

    // Se guardó como texto JSON; se devuelve como objeto para que el cliente no lo parsee dos veces.
    private static JsonNode json(String texto, ObjectMapper objectMapper) {
        if (texto == null) {
            return null;
        }
        try {
            return objectMapper.readTree(texto);
        } catch (Exception e) {
            throw new IllegalStateException("El historial tiene un JSON ilegible", e);
        }
    }
}
