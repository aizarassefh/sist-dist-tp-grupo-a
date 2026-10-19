package com.unla.museo.reportes.controller;

import com.unla.museo.eventos.util.EstadoEvento;
import com.unla.museo.eventos.service.EventoService;
import com.unla.museo.eventos.util.TipoEvento;
import com.unla.museo.eventos.dto.FilaReporteEventoDTO;
import com.unla.museo.reportes.util.ExportadorExcelReporte;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reportes", description = "Reportes de asistencia a eventos")
public class ReporteAsistenciaExcelController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final EventoService eventoService;
    private final ExportadorExcelReporte exportador;

    public ReporteAsistenciaExcelController(EventoService eventoService, ExportadorExcelReporte exportador) {
        this.eventoService = eventoService;
        this.exportador = exportador;
    }

    @Operation(summary = "Exportar el reporte de asistencia a Excel, una hoja por tipo de evento")
    @PreAuthorize("hasAnyRole('CURADOR','ADMINISTRADOR')")
    @GetMapping("/asistencia/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) TipoEvento tipo,
            @RequestParam(required = false) EstadoEvento estado) {
        List<FilaReporteEventoDTO> filas = eventoService.obtenerFilasParaReporte(desde, hasta, tipo, estado);
        byte[] contenido = exportador.exportar(filas, tipo);

        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-asistencia.xlsx\"")
                .body(contenido);
    }
}
