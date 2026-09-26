package com.unla.museo.graphql;

import org.springframework.stereotype.Service;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReporteService {
	private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter FC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	public ReporteAsistencia generarReporte(FiltroReporte filtro) {
		List<EventoGraphQL> eventos = EventoData.obtenerEventos();
		LocalDateTime corte = LocalDateTime.now();
		eventos = eventos.stream().filter(e -> fecha(e, filtro)).filter(e -> tipo(e, filtro))
				.filter(e -> estado(e, filtro, corte)).toList();
		AgruparPor agrupacion = filtro != null && filtro.getAgruparPor() != null ? filtro.getAgruparPor()
				: AgruparPor.MES;
		Map<String, List<EventoGraphQL>> grupos = agrupar(eventos, agrupacion);
		List<GrupoReporte> res = new ArrayList<>();
		for (var entry : grupos.entrySet()) {
			String clave = entry.getKey();
			String mes = null, tipo = null;
			if (agrupacion == AgruparPor.MES)
				mes = clave;
			if (agrupacion == AgruparPor.TIPO)
				tipo = clave;
			if (agrupacion == AgruparPor.MES_Y_TIPO) {
				String[] p = clave.split("\\|", 2);
				mes = p[0];
				tipo = p[1];
			}
			List<EventoGraphQL> eg = entry.getValue();
			int cantidad = eg.size();
			int total = eg.stream().mapToInt(e -> e.getInscriptos() == null ? 0 : e.getInscriptos().size()).sum();
			double promedio = cantidad == 0 ? 0 : (double) total / cantidad;
			List<EventoPopular> pop = eg.stream()
					.sorted(Comparator
							.comparingInt((EventoGraphQL e) -> e.getInscriptos() == null ? 0 : e.getInscriptos().size())
							.reversed().thenComparing(EventoGraphQL::getFechaHora).thenComparing(EventoGraphQL::getId))
					.limit(3).map(e -> new EventoPopular(e.getId(), e.getTitulo(),
							e.getInscriptos() == null ? 0 : e.getInscriptos().size()))
					.toList();
			res.add(new GrupoReporte(mes, tipo, cantidad, total, Math.round(promedio * 100.0) / 100.0, pop));
		}
		return new ReporteAsistencia(corte.format(FC), res);
	}

	private boolean fecha(EventoGraphQL e, FiltroReporte f) {
		if (f == null)
			return true;
		if (f.getDesde() != null && !f.getDesde().isBlank()
				&& e.getFechaHora().isBefore(LocalDate.parse(f.getDesde(), F).atStartOfDay()))
			return false;
		if (f.getHasta() != null && !f.getHasta().isBlank()
				&& !e.getFechaHora().isBefore(LocalDate.parse(f.getHasta(), F).plusDays(1).atStartOfDay()))
			return false;
		return true;
	}

	private boolean tipo(EventoGraphQL e, FiltroReporte f) {
		return f == null || f.getTipo() == null || f.getTipo().isBlank()
				|| e.getTipo().equalsIgnoreCase(f.getTipo().trim());
	}

	private boolean estado(EventoGraphQL e, FiltroReporte f, LocalDateTime c) {
		if (f == null || f.getEstado() == null || f.getEstado() == EstadoEvento.TODOS)
			return true;
		return f.getEstado() == EstadoEvento.PASADOS ? e.getFechaHora().isBefore(c) : !e.getFechaHora().isBefore(c);
	}

	private Map<String, List<EventoGraphQL>> agrupar(List<EventoGraphQL> es, AgruparPor a) {
		Comparator<EventoGraphQL> c = Comparator.comparing(EventoGraphQL::getFechaHora)
				.thenComparing(EventoGraphQL::getId);
		List<EventoGraphQL> orden = es.stream().sorted(c).toList();
		if (a == AgruparPor.TIPO)
			return orden.stream()
					.collect(Collectors.groupingBy(EventoGraphQL::getTipo, LinkedHashMap::new, Collectors.toList()));
		if (a == AgruparPor.MES_Y_TIPO)
			return orden.stream()
					.collect(Collectors.groupingBy(e -> YearMonth.from(e.getFechaHora()) + "|" + e.getTipo(),
							LinkedHashMap::new, Collectors.toList()));
		return orden.stream().collect(Collectors.groupingBy(e -> YearMonth.from(e.getFechaHora()).toString(),
				LinkedHashMap::new, Collectors.toList()));
	}
}
