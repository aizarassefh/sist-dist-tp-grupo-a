package com.unla.museo.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

@Controller
public class GraphQLController {
	private final ReporteService reporteService;

	public GraphQLController(ReporteService reporteService) {
		this.reporteService = reporteService;
	}

	@QueryMapping
	public String saludo() {
		return "GraphQL funcionando correctamente";
	}

	@QueryMapping
	public List<Obra> obras(@Argument FiltroObras filtro, @Argument Integer pagina, @Argument Integer tamanio) {
		List<Obra> obras = ObraData.filtrarObras(filtro);
		int p = pagina != null ? Math.max(pagina, 0) : 0;
		int t = tamanio != null ? Math.min(Math.max(tamanio, 1), 100) : 20;
		int inicio = p * t;
		if (inicio >= obras.size())
			return List.of();
		return obras.subList(inicio, Math.min(inicio + t, obras.size()));
	}

	@QueryMapping
	public ReporteAsistencia reporteAsistencia(@Argument FiltroReporte filtro) {
		requireRole("CURADOR", "ADMINISTRADOR");
		return reporteService.generarReporte(filtro);
	}

	private void requireRole(String... roles) {
		Authentication a = SecurityContextHolder.getContext().getAuthentication();
		if (a == null || !a.isAuthenticated())
			throw new org.springframework.security.access.AccessDeniedException("Autenticación requerida");
		boolean ok = a.getAuthorities().stream().anyMatch(x -> {
			String r = x.getAuthority();
			return java.util.Arrays.stream(roles).anyMatch(role -> r.equals("ROLE_" + role));
		});
		if (!ok)
			throw new AccessDeniedException("User role is not authorized to perform this action.");
	}
}
