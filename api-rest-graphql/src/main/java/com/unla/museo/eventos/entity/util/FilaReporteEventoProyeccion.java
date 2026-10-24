package com.unla.museo.eventos.entity.util;

import com.unla.museo.eventos.util.TipoEvento;

import java.time.LocalDateTime;

/**
 * Proyección de la consulta de reporte: una fila por evento con el conteo de
 * inscriptos ya agrupado (LEFT JOIN + COUNT), para no traer las entidades
 * completas ni pagar un N+1 por curador.
 */
public interface FilaReporteEventoProyeccion {
    Long getId();
    String getTitulo();
    TipoEvento getTipo();
    LocalDateTime getFechaHora();
    Long getCuradorId();
    String getCuradorNombre();
    String getCuradorApellido();
    Integer getCupoMaximo();
    Long getCantidadInscriptos();
}
