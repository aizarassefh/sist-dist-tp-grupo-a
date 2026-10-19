package com.unla.museo.eventos.dto;

import com.unla.museo.eventos.util.TipoEvento;

import java.time.LocalDateTime;

/**
 * Una fila por evento para el reporte de asistencia y su exportación a
 * Excel: ambos consumen exactamente esta misma lista (vía
 * EventoService.obtenerFilasParaReporte) para que nunca den números
 * distintos.
 */
public record FilaReporteEventoDTO(
        Long id,
        String titulo,
        TipoEvento tipo,
        LocalDateTime fechaHora,
        PersonaDTO curador,
        Integer cupoMaximo,
        long cantidadInscriptos
) {
}
