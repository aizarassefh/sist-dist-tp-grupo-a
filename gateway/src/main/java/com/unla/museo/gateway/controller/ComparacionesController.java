package com.unla.museo.gateway.controller;

import com.unla.museo.catalogo.grpc.ObtenerObrasRequest;
import com.unla.museo.gateway.error.ErrorGateway;
import com.unla.museo.gateway.grpc.ServiciosGrpc;
import com.unla.museo.gateway.grpc.Trazas;
import com.unla.museo.gateway.json.MapeadorJson;
import com.unla.museo.gateway.json.MapeadorJson.ObraJson;
import com.unla.museo.motor.grpc.CompararObrasRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;

/** POST /api/v2/comparaciones: Catálogo -> Motor. */
@RestController
@RequestMapping("/api/v2/comparaciones")
public class ComparacionesController {

    public record PedidoComparacion(List<Long> obraIds) {
    }

    public record AtributoJson(String nombre, List<String> valores, boolean coinciden) {
    }

    public record RespuestaComparacion(List<ObraJson> obras, List<AtributoJson> atributos,
                                       List<String> observaciones, List<Trazas.Traza> trazas) {
    }

    private final ServiciosGrpc servicios;

    public ComparacionesController(ServiciosGrpc servicios) {
        this.servicios = servicios;
    }

    @PostMapping
    public Mono<RespuestaComparacion> comparar(@RequestBody PedidoComparacion pedido) {
        List<Long> ids = pedido.obraIds();
        if (ids == null || ids.size() < 2 || ids.size() > 6 || ids.contains(null)
                || new HashSet<>(ids).size() != ids.size()) {
            throw ErrorGateway.pedidoInvalido("obraIds debe tener entre 2 y 6 ids distintos");
        }

        Trazas trazas = new Trazas();
        return servicios.obtenerObras(trazas, ObtenerObrasRequest.newBuilder().addAllIds(ids).build())
                .flatMap(obras -> servicios.compararObras(trazas,
                                CompararObrasRequest.newBuilder().addAllObras(obras.getObrasList()).build())
                        .map(comparacion -> new RespuestaComparacion(
                                MapeadorJson.obras(obras.getObrasList()),
                                comparacion.getAtributosList().stream()
                                        .map(a -> new AtributoJson(a.getNombre(), List.copyOf(a.getValoresList()), a.getCoinciden()))
                                        .toList(),
                                List.copyOf(comparacion.getObservacionesList()),
                                trazas.comoLista())));
    }
}
