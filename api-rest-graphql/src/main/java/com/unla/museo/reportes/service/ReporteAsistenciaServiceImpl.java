package com.unla.museo.reportes.service;

import com.unla.museo.comun.errores.SolicitudInvalidaException;
import com.unla.museo.eventos.util.EstadoEvento;
import com.unla.museo.eventos.service.EventoService;
import com.unla.museo.eventos.util.TipoEvento;
import com.unla.museo.eventos.dto.FilaReporteEventoDTO;
import com.unla.museo.reportes.util.AgruparPor;
import com.unla.museo.reportes.dto.EventoPopularDTO;
import com.unla.museo.reportes.dto.GrupoReporteDTO;
import com.unla.museo.reportes.dto.ReporteAsistenciaDTO;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReporteAsistenciaServiceImpl implements ReporteAsistenciaService {

    private static final DateTimeFormatter FORMATO_FECHA_CORTE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final EventoService eventoService;
    private final Clock clock;

    public ReporteAsistenciaServiceImpl(EventoService eventoService, Clock clock) {
        this.eventoService = eventoService;
        this.clock = clock;
    }

    @Override
    public ReporteAsistenciaDTO generar(String desdeTexto, String hastaTexto, TipoEvento tipo, EstadoEvento estado,
                                        AgruparPor agruparPor) {
        LocalDate desde = parsearFecha(desdeTexto, "desde");
        LocalDate hasta = parsearFecha(hastaTexto, "hasta");

        // Misma fila que consume el Excel (EventoService.obtenerFilasParaReporte):
        // el reporte GraphQL y el .xlsx nunca dan números distintos.
        List<FilaReporteEventoDTO> filas = eventoService.obtenerFilasParaReporte(desde, hasta, tipo, estado);
        AgruparPor agrupacionEfectiva = agruparPor != null ? agruparPor : AgruparPor.MES;
        List<GrupoReporteDTO> grupos = agrupar(filas, agrupacionEfectiva);
        String fechaCorte = LocalDateTime.now(clock).format(FORMATO_FECHA_CORTE);
        return new ReporteAsistenciaDTO(fechaCorte, grupos);
    }

    private LocalDate parsearFecha(String texto, String nombreCampo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException ex) {
            throw new SolicitudInvalidaException("La fecha '" + nombreCampo + "' debe tener el formato AAAA-MM-DD");
        }
    }

    private List<GrupoReporteDTO> agrupar(List<FilaReporteEventoDTO> filas, AgruparPor agrupacion) {
        Map<ClaveGrupo, List<FilaReporteEventoDTO>> porClave = new LinkedHashMap<>();
        for (FilaReporteEventoDTO fila : filas) {
            ClaveGrupo clave = clave(fila, agrupacion);
            porClave.computeIfAbsent(clave, k -> new ArrayList<>()).add(fila);
        }

        Comparator<ClaveGrupo> orden = Comparator
                .comparing(ClaveGrupo::mes, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ClaveGrupo::tipo, Comparator.nullsFirst(Comparator.naturalOrder()));

        return porClave.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(orden))
                .map(entrada -> construirGrupo(entrada.getKey(), entrada.getValue()))
                .toList();
    }

    private ClaveGrupo clave(FilaReporteEventoDTO fila, AgruparPor agrupacion) {
        return switch (agrupacion) {
            case MES -> new ClaveGrupo(YearMonth.from(fila.fechaHora()), null);
            case TIPO -> new ClaveGrupo(null, fila.tipo());
            case MES_Y_TIPO -> new ClaveGrupo(YearMonth.from(fila.fechaHora()), fila.tipo());
        };
    }

    private GrupoReporteDTO construirGrupo(ClaveGrupo clave, List<FilaReporteEventoDTO> filas) {
        int cantidadDeEventos = filas.size();
        int totalInscriptos = filas.stream().mapToInt(f -> (int) f.cantidadInscriptos()).sum();
        double promedio = Math.round((double) totalInscriptos / cantidadDeEventos * 100.0) / 100.0;

        List<EventoPopularDTO> populares = filas.stream()
                .filter(f -> f.cantidadInscriptos() > 0)
                .sorted(Comparator.comparingLong(FilaReporteEventoDTO::cantidadInscriptos).reversed()
                        .thenComparing(FilaReporteEventoDTO::fechaHora)
                        .thenComparing(FilaReporteEventoDTO::id))
                .limit(3)
                .map(f -> new EventoPopularDTO(f.id(), f.titulo(), (int) f.cantidadInscriptos()))
                .toList();

        String mes = clave.mes() != null ? clave.mes().toString() : null;
        return new GrupoReporteDTO(mes, clave.tipo(), cantidadDeEventos, totalInscriptos, promedio, populares);
    }

    /** Clave interna de agrupamiento: mes, tipo o ambos, según AgruparPor. */
    private record ClaveGrupo(YearMonth mes, TipoEvento tipo) {
    }
}
