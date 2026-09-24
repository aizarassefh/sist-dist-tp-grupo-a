package com.unla.museo.graphql;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter FECHA_CORTE_FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public ReporteAsistencia generarReporte(FiltroReporte filtro) {

        List<EventoGraphQL> eventos = EventoData.obtenerEventos();

        LocalDateTime fechaCorte = LocalDateTime.now();

        // ---------------------------------------------------------
        // 1. Aplicar filtros
        // ---------------------------------------------------------

        eventos = eventos.stream()
                .filter(evento -> cumpleFiltroFecha(evento, filtro))
                .filter(evento -> cumpleFiltroTipo(evento, filtro))
                .filter(evento -> cumpleFiltroEstado(evento, filtro, fechaCorte))
                .toList();

        // ---------------------------------------------------------
        // 2. Determinar agrupación
        // ---------------------------------------------------------

        String agruparPor = "MES";

        if (filtro != null
                && filtro.getAgruparPor() != null
                && !filtro.getAgruparPor().isBlank()) {

            agruparPor = filtro.getAgruparPor().toUpperCase();
        }

        // ---------------------------------------------------------
        // 3. Agrupar eventos
        // ---------------------------------------------------------

        Map<String, List<EventoGraphQL>> grupos =
                agruparEventos(eventos, agruparPor);

        // ---------------------------------------------------------
        // 4. Construir resultado
        // ---------------------------------------------------------

        List<GrupoReporte> resultados = new ArrayList<>();

        for (Map.Entry<String, List<EventoGraphQL>> entrada
                : grupos.entrySet()) {

            String clave = entrada.getKey();

            List<EventoGraphQL> eventosGrupo =
                    entrada.getValue();

            String mes = null;
            String tipo = null;

            // Agrupación por mes
            if (agruparPor.equals("MES")) {

                mes = clave;
            }

            // Agrupación por tipo
            if (agruparPor.equals("TIPO")) {

                tipo = clave;
            }

            // Agrupación por mes y tipo
            if (agruparPor.equals("MES_Y_TIPO")) {

                String[] partes = clave.split("\\|", 2);

                mes = partes[0];

                if (partes.length > 1) {
                    tipo = partes[1];
                }
            }

            // -----------------------------------------------------
            // Cantidad de eventos
            // -----------------------------------------------------

            int cantidadEventos = eventosGrupo.size();

            // -----------------------------------------------------
            // Total de inscriptos
            // -----------------------------------------------------

            int totalInscriptos = eventosGrupo.stream()
                    .mapToInt(evento ->
                            evento.getInscriptos() == null
                                    ? 0
                                    : evento.getInscriptos().size()
                    )
                    .sum();

            // -----------------------------------------------------
            // Promedio de inscriptos por evento
            // -----------------------------------------------------

            double promedio = cantidadEventos == 0
                    ? 0
                    : (double) totalInscriptos / cantidadEventos;

            // -----------------------------------------------------
            // Eventos más populares
            // -----------------------------------------------------

            List<EventoPopular> populares =
                    eventosGrupo.stream()
                            .sorted(
                                    Comparator
                                            .comparingInt(
                                                    (EventoGraphQL evento) ->
                                                            evento.getInscriptos() == null
                                                                    ? 0
                                                                    : evento.getInscriptos().size()
                                            )
                                            .reversed()
                                            .thenComparing(
                                                    EventoGraphQL::getFechaHora
                                            )
                                            .thenComparing(
                                                    EventoGraphQL::getId
                                            )
                            )
                            .limit(3)
                            .map(evento ->
                                    new EventoPopular(
                                            evento.getId(),
                                            evento.getTitulo(),
                                            evento.getInscriptos() == null
                                                    ? 0
                                                    : evento.getInscriptos().size()
                                    )
                            )
                            .toList();

            // -----------------------------------------------------
            // Crear grupo del reporte
            // -----------------------------------------------------

            resultados.add(
                    new GrupoReporte(
                            mes,
                            tipo,
                            cantidadEventos,
                            totalInscriptos,
                            Math.round(promedio * 100.0) / 100.0,
                            populares
                    )
            );
        }

        // ---------------------------------------------------------
        // 5. Devolver reporte
        // ---------------------------------------------------------

        return new ReporteAsistencia(
                fechaCorte.format(FECHA_CORTE_FORMATO),
                resultados
        );
    }

    // =============================================================
    // FILTRO POR FECHA
    // =============================================================

    private boolean cumpleFiltroFecha(
            EventoGraphQL evento,
            FiltroReporte filtro) {

        if (filtro == null) {
            return true;
        }

        // Desde
        if (filtro.getDesde() != null
                && !filtro.getDesde().isBlank()) {

            LocalDate desde =
                    LocalDate.parse(
                            filtro.getDesde(),
                            FECHA_FORMATO
                    );

            if (evento.getFechaHora()
                    .isBefore(desde.atStartOfDay())) {

                return false;
            }
        }

        // Hasta
        if (filtro.getHasta() != null
                && !filtro.getHasta().isBlank()) {

            LocalDate hasta =
                    LocalDate.parse(
                            filtro.getHasta(),
                            FECHA_FORMATO
                    );

            // Se incluye todo el día "hasta".
            LocalDateTime inicioDiaSiguiente =
                    hasta.plusDays(1).atStartOfDay();

            if (!evento.getFechaHora()
                    .isBefore(inicioDiaSiguiente)) {

                return false;
            }
        }

        return true;
    }

    // =============================================================
    // FILTRO POR TIPO
    // =============================================================

    private boolean cumpleFiltroTipo(
            EventoGraphQL evento,
            FiltroReporte filtro) {

        if (filtro == null
                || filtro.getTipo() == null
                || filtro.getTipo().isBlank()) {

            return true;
        }

        return evento.getTipo()
                .equalsIgnoreCase(
                        filtro.getTipo().trim()
                );
    }

    // =============================================================
    // FILTRO POR ESTADO
    // =============================================================

    private boolean cumpleFiltroEstado(
            EventoGraphQL evento,
            FiltroReporte filtro,
            LocalDateTime fechaCorte) {

        if (filtro == null
                || filtro.getEstado() == null
                || filtro.getEstado().isBlank()
                || filtro.getEstado().equalsIgnoreCase("TODOS")) {

            return true;
        }

        if (filtro.getEstado()
                .equalsIgnoreCase("PASADOS")) {

            return evento.getFechaHora()
                    .isBefore(fechaCorte);
        }

        if (filtro.getEstado()
                .equalsIgnoreCase("FUTUROS")) {

            return !evento.getFechaHora()
                    .isBefore(fechaCorte);
        }

        return true;
    }

    // =============================================================
    // AGRUPACIÓN
    // =============================================================

    private Map<String, List<EventoGraphQL>> agruparEventos(
            List<EventoGraphQL> eventos,
            String agruparPor) {

        // ---------------------------------------------------------
        // Agrupar por tipo
        // ---------------------------------------------------------

        if (agruparPor.equals("TIPO")) {

            return eventos.stream()
                    .collect(
                            Collectors.groupingBy(
                                    EventoGraphQL::getTipo,
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            )
                    );
        }

        // ---------------------------------------------------------
        // Agrupar por mes y tipo
        // ---------------------------------------------------------

        if (agruparPor.equals("MES_Y_TIPO")) {

            return eventos.stream()
                    .collect(
                            Collectors.groupingBy(
                                    evento ->
                                            YearMonth
                                                    .from(evento.getFechaHora())
                                                    .toString()
                                                    + "|"
                                                    + evento.getTipo(),
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            )
                    );
        }

        // ---------------------------------------------------------
        // Agrupar por mes
        // ---------------------------------------------------------

        return eventos.stream()
                .collect(
                        Collectors.groupingBy(
                                evento ->
                                        YearMonth
                                                .from(evento.getFechaHora())
                                                .toString(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                );
    }
}