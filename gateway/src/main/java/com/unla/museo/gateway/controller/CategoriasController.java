package com.unla.museo.gateway.controller;

import com.unla.museo.gateway.grpc.ServiciosGrpc;
import com.unla.museo.gateway.grpc.Trazas;
import com.unla.museo.gateway.json.MapeadorJson;
import com.unla.museo.gateway.json.MapeadorJson.CategoriaJson;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/** GET /api/v2/categorias: reenvía CatalogoService.ListarCategorias. */
@RestController
@RequestMapping("/api/v2/categorias")
public class CategoriasController {

    public record RespuestaCategorias(List<CategoriaJson> movimientos, List<CategoriaJson> tecnicas,
                                      List<CategoriaJson> tematicas) {
    }

    private final ServiciosGrpc servicios;

    public CategoriasController(ServiciosGrpc servicios) {
        this.servicios = servicios;
    }

    @GetMapping
    public Mono<RespuestaCategorias> listar() {
        // Esta respuesta no lleva trazas según el contrato, pero la llamada exige un registro.
        return servicios.listarCategorias(new Trazas())
                .map(c -> new RespuestaCategorias(
                        MapeadorJson.categorias(c.getMovimientosList()),
                        MapeadorJson.categorias(c.getTecnicasList()),
                        MapeadorJson.categorias(c.getTematicasList())));
    }
}
