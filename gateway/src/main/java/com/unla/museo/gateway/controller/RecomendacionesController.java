package com.unla.museo.gateway.controller;

import com.unla.museo.catalogo.grpc.BuscarObrasRequest;
import com.unla.museo.catalogo.grpc.Obra;
import com.unla.museo.catalogo.grpc.ObrasResponse;
import com.unla.museo.catalogo.grpc.ObtenerObrasRequest;
import com.unla.museo.gateway.error.ErrorGateway;
import com.unla.museo.gateway.grpc.ServiciosGrpc;
import com.unla.museo.gateway.grpc.Trazas;
import com.unla.museo.gateway.json.MapeadorJson;
import com.unla.museo.gateway.json.MapeadorJson.ObraJson;
import com.unla.museo.gateway.json.MapeadorJson.PreferenciasJson;
import com.unla.museo.motor.grpc.RecomendarRequest;
import com.unla.museo.usuarios.grpc.Interaccion;
import com.unla.museo.usuarios.grpc.Preferencias;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static com.unla.museo.gateway.seguridad.FiltroJwt.ATRIBUTO_EMAIL;

/**
 * GET /api/v2/recomendaciones: (Perfil, Interacciones, Eventos del usuario) en
 * paralelo -> (todas las obras, obras de las interacciones) en paralelo -> Motor.
 */
@RestController
@RequestMapping("/api/v2/recomendaciones")
public class RecomendacionesController {

    public record RecomendacionJson(ObraJson obra, double puntaje, List<String> motivos) {
    }

    public record RespuestaRecomendaciones(List<RecomendacionJson> recomendaciones, String criterio,
                                           PreferenciasJson preferencias, List<Trazas.Traza> trazas) {
    }

    private final ServiciosGrpc servicios;

    public RecomendacionesController(ServiciosGrpc servicios) {
        this.servicios = servicios;
    }

    @GetMapping
    public Mono<RespuestaRecomendaciones> recomendar(@RequestParam(defaultValue = "6") int cantidad,
                                                     @RequestAttribute(ATRIBUTO_EMAIL) String email) {
        if (cantidad < 1 || cantidad > 20) {
            throw ErrorGateway.pedidoInvalido("cantidad debe estar entre 1 y 20");
        }

        Trazas trazas = new Trazas();
        return Mono.zip(
                        servicios.obtenerPerfil(trazas, email),
                        servicios.listarInteracciones(trazas, email),
                        servicios.listarEventosDeUsuario(trazas, email))
                .flatMap(datosUsuario -> {
                    Preferencias preferencias = datosUsuario.getT1().getPreferencias();
                    List<Interaccion> interacciones = datosUsuario.getT2().getInteraccionesList();

                    // Las obras repetidas en las interacciones se piden una sola vez.
                    LinkedHashSet<Long> idsInteraccion = new LinkedHashSet<>();
                    interacciones.forEach(i -> idsInteraccion.add(i.getObraId()));

                    Mono<ObrasResponse> conInteraccion = idsInteraccion.isEmpty()
                            ? Mono.just(ObrasResponse.getDefaultInstance())
                            : servicios.obtenerObras(trazas, ObtenerObrasRequest.newBuilder().addAllIds(idsInteraccion).build());

                    return Mono.zip(servicios.buscarObras(trazas, BuscarObrasRequest.getDefaultInstance()), conInteraccion)
                            .flatMap(obras -> {
                                RecomendarRequest pedido = RecomendarRequest.newBuilder()
                                        .addAllCandidatas(obras.getT1().getObrasList())
                                        .setPreferencias(preferencias)
                                        .addAllInteracciones(interacciones)
                                        .addAllObrasConInteraccion(obras.getT2().getObrasList())
                                        .addAllEventosInscriptos(datosUsuario.getT3().getEventosList())
                                        .setCantidad(cantidad)
                                        .build();
                                return servicios.recomendar(trazas, pedido).map(respuesta -> {
                                    Map<Long, Obra> porId = new HashMap<>();
                                    obras.getT2().getObrasList().forEach(o -> porId.put(o.getId(), o));
                                    obras.getT1().getObrasList().forEach(o -> porId.put(o.getId(), o));
                                    List<RecomendacionJson> lista = respuesta.getRecomendacionesList().stream()
                                            .map(r -> new RecomendacionJson(
                                                    porId.containsKey(r.getObraId()) ? MapeadorJson.obra(porId.get(r.getObraId())) : null,
                                                    r.getPuntaje(), List.copyOf(r.getMotivosList())))
                                            .toList();
                                    return new RespuestaRecomendaciones(lista, respuesta.getCriterio(),
                                            MapeadorJson.preferencias(preferencias), trazas.comoLista());
                                });
                            });
                });
    }
}
