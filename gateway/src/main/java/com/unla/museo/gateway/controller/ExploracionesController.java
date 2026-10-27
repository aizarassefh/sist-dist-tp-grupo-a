package com.unla.museo.gateway.controller;

import com.unla.museo.catalogo.grpc.BuscarObrasRequest;
import com.unla.museo.catalogo.grpc.ListarArtistasRequest;
import com.unla.museo.eventos.grpc.EventosPorMovimientoRequest;
import com.unla.museo.gateway.grpc.ServiciosGrpc;
import com.unla.museo.gateway.grpc.Trazas;
import com.unla.museo.gateway.json.MapeadorJson;
import com.unla.museo.gateway.json.MapeadorJson.ArtistaJson;
import com.unla.museo.gateway.json.MapeadorJson.CategoriaJson;
import com.unla.museo.gateway.json.MapeadorJson.EventoJson;
import com.unla.museo.gateway.json.MapeadorJson.ObraJson;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/** GET /api/v2/exploraciones/{movimiento}: Catálogo (x3) y Eventos en paralelo; no usa el motor. */
@RestController
@RequestMapping("/api/v2/exploraciones")
public class ExploracionesController {

    public record RespuestaExploracion(String movimiento, List<ObraJson> obras, List<ArtistaJson> artistas,
                                       List<EventoJson> eventos, List<CategoriaJson> otrosMovimientos,
                                       List<Trazas.Traza> trazas) {
    }

    private final ServiciosGrpc servicios;

    public ExploracionesController(ServiciosGrpc servicios) {
        this.servicios = servicios;
    }

    @GetMapping("/{movimiento}")
    public Mono<RespuestaExploracion> explorar(@PathVariable String movimiento) {
        Trazas trazas = new Trazas();
        return Mono.zip(
                        servicios.buscarObras(trazas, BuscarObrasRequest.newBuilder().addMovimientos(movimiento).build()),
                        servicios.listarArtistas(trazas, ListarArtistasRequest.newBuilder().setMovimiento(movimiento).build()),
                        servicios.listarEventosPorMovimiento(trazas, EventosPorMovimientoRequest.newBuilder()
                                .setMovimiento(movimiento).setSoloFuturos(true).build()),
                        servicios.listarCategorias(trazas))
                .map(r -> new RespuestaExploracion(
                        movimiento,
                        MapeadorJson.obras(r.getT1().getObrasList()),
                        r.getT2().getArtistasList().stream().map(MapeadorJson::artista).toList(),
                        r.getT3().getEventosList().stream().map(MapeadorJson::evento).toList(),
                        // Las categorías traen todos los movimientos; "otros" son los que no son el explorado.
                        MapeadorJson.categorias(r.getT4().getMovimientosList()).stream()
                                .filter(c -> !c.nombre().equalsIgnoreCase(movimiento))
                                .toList(),
                        trazas.comoLista()));
    }
}
