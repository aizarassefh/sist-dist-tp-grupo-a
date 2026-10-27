package com.unla.museo.gateway.controller;

import com.unla.museo.catalogo.grpc.ObtenerObrasRequest;
import com.unla.museo.gateway.error.ErrorGateway;
import com.unla.museo.gateway.grpc.ServiciosGrpc;
import com.unla.museo.gateway.grpc.Trazas;
import com.unla.museo.gateway.json.MapeadorJson;
import com.unla.museo.gateway.json.MapeadorJson.PreferenciasJson;
import com.unla.museo.usuarios.grpc.ActualizarPreferenciasRequest;
import com.unla.museo.usuarios.grpc.Perfil;
import com.unla.museo.usuarios.grpc.Preferencias;
import com.unla.museo.usuarios.grpc.RegistrarInteraccionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Set;

import static com.unla.museo.gateway.seguridad.FiltroJwt.ATRIBUTO_EMAIL;

/** Preferencias e interacciones del visitante: soporte de recorridos y recomendaciones. */
@RestController
@RequestMapping("/api/v2")
public class PreferenciasController {

    private static final Set<String> TIPOS_INTERACCION = Set.of("VISTA", "INTERES");

    public record PedidoInteraccion(Long obraId, String tipo) {
    }

    public record RespuestaInteraccion(long obraId, String tipo, String fecha) {
    }

    private final ServiciosGrpc servicios;

    public PreferenciasController(ServiciosGrpc servicios) {
        this.servicios = servicios;
    }

    @GetMapping("/preferencias")
    public Mono<PreferenciasJson> obtener(@RequestAttribute(ATRIBUTO_EMAIL) String email) {
        return servicios.obtenerPerfil(new Trazas(), email)
                .map(perfil -> MapeadorJson.preferencias(perfil.getPreferencias()));
    }

    @PutMapping("/preferencias")
    public Mono<PreferenciasJson> actualizar(@RequestBody PreferenciasJson pedido,
                                             @RequestAttribute(ATRIBUTO_EMAIL) String email) {
        Preferencias preferencias = Preferencias.newBuilder()
                .addAllMovimientos(MapeadorJson.listaSegura(pedido.movimientos()))
                .addAllTecnicas(MapeadorJson.listaSegura(pedido.tecnicas()))
                .addAllTematicas(MapeadorJson.listaSegura(pedido.tematicas()))
                .build();
        return servicios.actualizarPreferencias(new Trazas(), ActualizarPreferenciasRequest.newBuilder()
                        .setEmail(email).setPreferencias(preferencias).build())
                .map((Perfil perfil) -> MapeadorJson.preferencias(perfil.getPreferencias()));
    }

    @PostMapping("/interacciones")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RespuestaInteraccion> registrarInteraccion(@RequestBody PedidoInteraccion pedido,
                                                           @RequestAttribute(ATRIBUTO_EMAIL) String email) {
        if (pedido.obraId() == null || pedido.obraId() < 1) {
            throw ErrorGateway.pedidoInvalido("obraId es obligatorio");
        }
        if (pedido.tipo() == null || !TIPOS_INTERACCION.contains(pedido.tipo())) {
            throw ErrorGateway.pedidoInvalido("tipo debe ser VISTA o INTERES");
        }

        Trazas trazas = new Trazas();
        // Usuarios no valida la obra (la tabla es del catálogo): se verifica antes y,
        // si no existe, el NOT_FOUND corta el flujo sin llegar a registrar nada.
        return servicios.obtenerObras(trazas, ObtenerObrasRequest.newBuilder().addIds(pedido.obraId()).build())
                .then(Mono.defer(() -> servicios.registrarInteraccion(trazas, RegistrarInteraccionRequest.newBuilder()
                        .setEmail(email).setObraId(pedido.obraId()).setTipo(pedido.tipo()).build())))
                .map(i -> new RespuestaInteraccion(i.getObraId(), i.getTipo(), i.getFecha()));
    }
}
