package com.unla.museo.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

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
    public List<Obra> obras(
            @Argument FiltroObras filtro,
            @Argument Integer pagina,
            @Argument Integer tamanio) {

        List<Obra> obras = ObraData.filtrarObras(filtro);

        int paginaActual = pagina != null ? pagina : 0;
        int tamanioPagina = tamanio != null ? tamanio : 20;

        // Evitar páginas negativas.
        if (paginaActual < 0) {
            paginaActual = 0;
        }

        // Tamaño mínimo y máximo permitido.
        if (tamanioPagina < 1) {
            tamanioPagina = 20;
        }

        if (tamanioPagina > 100) {
            tamanioPagina = 100;
        }

        int inicio = paginaActual * tamanioPagina;

        // La página solicitada no contiene resultados.
        if (inicio >= obras.size()) {
            return List.of();
        }

        int fin = Math.min(
                inicio + tamanioPagina,
                obras.size()
        );

        return obras.subList(inicio, fin);
    }

    @QueryMapping
    public ReporteAsistencia reporteAsistencia(
            @Argument FiltroReporte filtro) {

        return reporteService.generarReporte(filtro);
    }
}