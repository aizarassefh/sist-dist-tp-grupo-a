package com.unla.museo.gateway.controller;

import com.unla.museo.catalogo.grpc.BuscarObrasRequest;
import com.unla.museo.catalogo.grpc.Obra;
import com.unla.museo.gateway.error.ErrorGateway;
import com.unla.museo.gateway.grpc.ServiciosGrpc;
import com.unla.museo.gateway.grpc.Trazas;
import com.unla.museo.gateway.json.MapeadorJson;
import com.unla.museo.gateway.json.MapeadorJson.ObraJson;
import com.unla.museo.motor.grpc.ArmarRecorridoRequest;
import com.unla.museo.motor.grpc.Recorrido;
import com.unla.museo.usuarios.grpc.Perfil;
import com.unla.museo.usuarios.grpc.Preferencias;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.unla.museo.gateway.seguridad.FiltroJwt.ATRIBUTO_EMAIL;

/** POST /api/v2/recorridos: Usuarios (opcional) -> Catálogo -> Motor. */
@RestController
@RequestMapping("/api/v2/recorridos")
public class RecorridosController {

    public record PedidoRecorrido(Integer duracionMinutos, List<String> movimientos, List<String> tecnicas,
                                  List<String> tematicas, Integer cantidadObras, Boolean usarPreferencias) {
    }

    public record ParadaJson(int orden, String sala, int minutosSugeridos, String motivo, ObraJson obra) {
    }

    public record RespuestaRecorrido(List<ParadaJson> paradas, int duracionTotalMinutos, List<String> notas,
                                     List<Trazas.Traza> trazas) {
    }

    private final ServiciosGrpc servicios;

    public RecorridosController(ServiciosGrpc servicios) {
        this.servicios = servicios;
    }

    @PostMapping
    public Mono<RespuestaRecorrido> generar(@RequestBody(required = false) PedidoRecorrido pedido,
                                            @RequestAttribute(ATRIBUTO_EMAIL) String email) {
        PedidoRecorrido p = pedido != null ? pedido
                : new PedidoRecorrido(null, null, null, null, null, null);
        validar(p);

        Trazas trazas = new Trazas();
        Mono<Preferencias> preferencias = Boolean.TRUE.equals(p.usarPreferencias())
                ? servicios.obtenerPerfil(trazas, email).map(Perfil::getPreferencias)
                : Mono.just(Preferencias.getDefaultInstance());

        BuscarObrasRequest busqueda = BuscarObrasRequest.newBuilder()
                .addAllMovimientos(MapeadorJson.listaSegura(p.movimientos()))
                .addAllTecnicas(MapeadorJson.listaSegura(p.tecnicas()))
                .addAllTematicas(MapeadorJson.listaSegura(p.tematicas()))
                .setSoloEnExhibicion(true)
                .build();

        return preferencias.flatMap(prefs ->
                servicios.buscarObras(trazas, busqueda).flatMap(candidatas -> {
                    ArmarRecorridoRequest.Builder armado = ArmarRecorridoRequest.newBuilder()
                            .addAllCandidatas(candidatas.getObrasList())
                            .setDuracionMinutos(p.duracionMinutos() != null ? p.duracionMinutos() : 0)
                            .setCantidadMaxima(p.cantidadObras() != null ? p.cantidadObras() : 0);
                    // Sin usarPreferencias no se manda el campo: el motor no tiene que enterarse de ellas.
                    if (Boolean.TRUE.equals(p.usarPreferencias())) {
                        armado.setPreferencias(prefs);
                    }
                    return servicios.armarRecorrido(trazas, armado.build())
                            .map(recorrido -> armarRespuesta(recorrido, candidatas.getObrasList(), trazas));
                }));
    }

    private void validar(PedidoRecorrido p) {
        if (p.duracionMinutos() != null && (p.duracionMinutos() < 15 || p.duracionMinutos() > 480)) {
            throw ErrorGateway.pedidoInvalido("duracionMinutos debe estar entre 15 y 480");
        }
        if (p.cantidadObras() != null && (p.cantidadObras() < 1 || p.cantidadObras() > 30)) {
            throw ErrorGateway.pedidoInvalido("cantidadObras debe estar entre 1 y 30");
        }
    }

    private RespuestaRecorrido armarRespuesta(Recorrido recorrido, List<Obra> candidatas, Trazas trazas) {
        Map<Long, Obra> porId = candidatas.stream().collect(Collectors.toMap(Obra::getId, Function.identity(), (a, b) -> a));
        List<ParadaJson> paradas = recorrido.getParadasList().stream()
                .map(pa -> new ParadaJson(pa.getOrden(), pa.getSala(), pa.getMinutosSugeridos(), pa.getMotivo(),
                        porId.containsKey(pa.getObraId()) ? MapeadorJson.obra(porId.get(pa.getObraId())) : null))
                .toList();
        return new RespuestaRecorrido(paradas, recorrido.getDuracionTotalMinutos(),
                List.copyOf(recorrido.getNotasList()), trazas.comoLista());
    }
}
