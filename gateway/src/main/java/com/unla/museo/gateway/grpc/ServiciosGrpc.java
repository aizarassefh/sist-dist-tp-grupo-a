package com.unla.museo.gateway.grpc;

import com.unla.museo.catalogo.grpc.*;
import com.unla.museo.eventos.grpc.*;
import com.unla.museo.gateway.error.ErrorGateway;
import com.unla.museo.motor.grpc.*;
import com.unla.museo.usuarios.grpc.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.AbstractStub;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Único lugar donde el gateway habla gRPC. Cada método es una llamada a un
 * servicio interno; hace tres cosas comunes: la ejecuta con deadline, anota la
 * traza y traduce los errores gRPC al HTTP del contrato.
 *
 * Los stubs son bloqueantes y se corren en boundedElastic: así Mono.zip
 * paraleliza las llamadas sin bloquear los hilos de Netty de WebFlux.
 */
@Component
@Slf4j
public class ServiciosGrpc {

    private static final String CATALOGO = "CatalogoService";
    private static final String EVENTOS = "EventosService";
    private static final String USUARIOS = "UsuariosService";
    private static final String MOTOR = "MotorService";

    @GrpcClient("catalogo")
    private CatalogoServiceGrpc.CatalogoServiceBlockingStub catalogo;

    @GrpcClient("eventos")
    private EventosServiceGrpc.EventosServiceBlockingStub eventos;

    @GrpcClient("usuarios")
    private UsuariosServiceGrpc.UsuariosServiceBlockingStub usuarios;

    @GrpcClient("motor")
    private MotorServiceGrpc.MotorServiceBlockingStub motor;

    @Value("${gateway.grpc.deadline-ms:5000}")
    private long deadlineMs;

    // ---------- Catálogo ----------

    public Mono<ObrasResponse> buscarObras(Trazas t, BuscarObrasRequest r) {
        return llamar(t, CATALOGO, "BuscarObras", catalogo, s -> s.buscarObras(r));
    }

    public Mono<ObrasResponse> obtenerObras(Trazas t, ObtenerObrasRequest r) {
        return llamar(t, CATALOGO, "ObtenerObras", catalogo, s -> s.obtenerObras(r));
    }

    public Mono<ArtistasResponse> listarArtistas(Trazas t, ListarArtistasRequest r) {
        return llamar(t, CATALOGO, "ListarArtistas", catalogo, s -> s.listarArtistas(r));
    }

    public Mono<CategoriasResponse> listarCategorias(Trazas t) {
        return llamar(t, CATALOGO, "ListarCategorias", catalogo,
                s -> s.listarCategorias(ListarCategoriasRequest.getDefaultInstance()));
    }

    // ---------- Eventos ----------

    public Mono<EventosResponse> listarEventosPorMovimiento(Trazas t, EventosPorMovimientoRequest r) {
        return llamar(t, EVENTOS, "ListarEventosPorMovimiento", eventos, s -> s.listarEventosPorMovimiento(r));
    }

    public Mono<EventosResponse> listarEventosDeUsuario(Trazas t, String email) {
        EventosDeUsuarioRequest r = EventosDeUsuarioRequest.newBuilder().setEmail(email).build();
        return llamar(t, EVENTOS, "ListarEventosDeUsuario", eventos, s -> s.listarEventosDeUsuario(r));
    }

    // ---------- Usuarios ----------

    public Mono<Perfil> obtenerPerfil(Trazas t, String email) {
        PerfilRequest r = PerfilRequest.newBuilder().setEmail(email).build();
        return llamar(t, USUARIOS, "ObtenerPerfil", usuarios, s -> s.obtenerPerfil(r));
    }

    public Mono<Perfil> actualizarPreferencias(Trazas t, ActualizarPreferenciasRequest r) {
        return llamar(t, USUARIOS, "ActualizarPreferencias", usuarios, s -> s.actualizarPreferencias(r));
    }

    public Mono<Interaccion> registrarInteraccion(Trazas t, RegistrarInteraccionRequest r) {
        return llamar(t, USUARIOS, "RegistrarInteraccion", usuarios, s -> s.registrarInteraccion(r));
    }

    public Mono<InteraccionesResponse> listarInteracciones(Trazas t, String email) {
        PerfilRequest r = PerfilRequest.newBuilder().setEmail(email).build();
        return llamar(t, USUARIOS, "ListarInteracciones", usuarios, s -> s.listarInteracciones(r));
    }

    // ---------- Motor ----------

    public Mono<Recorrido> armarRecorrido(Trazas t, ArmarRecorridoRequest r) {
        return llamar(t, MOTOR, "ArmarRecorrido", motor, s -> s.armarRecorrido(r));
    }

    public Mono<RecomendacionesResponse> recomendar(Trazas t, RecomendarRequest r) {
        return llamar(t, MOTOR, "Recomendar", motor, s -> s.recomendar(r));
    }

    public Mono<Comparacion> compararObras(Trazas t, CompararObrasRequest r) {
        return llamar(t, MOTOR, "CompararObras", motor, s -> s.compararObras(r));
    }

    private <S extends AbstractStub<S>, T> Mono<T> llamar(
            Trazas trazas, String servicio, String metodo, S stub, Function<S, T> invocar) {
        return Mono.fromCallable(() -> {
            long inicio = System.nanoTime();
            try {
                // El deadline se arma acá y no al crear el mono: corre desde que
                // la llamada empieza, no desde que se armó el pipeline.
                T respuesta = invocar.apply(stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS));
                trazas.registrar(servicio, metodo, (System.nanoTime() - inicio) / 1_000_000);
                return respuesta;
            } catch (StatusRuntimeException e) {
                throw traducir(servicio, metodo, e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private ErrorGateway traducir(String servicio, String metodo, StatusRuntimeException e) {
        Status.Code codigo = e.getStatus().getCode();
        String descripcion = e.getStatus().getDescription();
        return switch (codigo) {
            case INVALID_ARGUMENT -> new ErrorGateway(HttpStatus.BAD_REQUEST,
                    descripcion != null ? descripcion : "El pedido no es válido");
            case NOT_FOUND -> new ErrorGateway(HttpStatus.NOT_FOUND,
                    descripcion != null ? descripcion : "No se encontró lo pedido");
            case UNAVAILABLE, DEADLINE_EXCEEDED -> {
                log.warn("{}.{} no respondió ({})", servicio, metodo, codigo);
                yield new ErrorGateway(HttpStatus.SERVICE_UNAVAILABLE,
                        "El servicio " + servicio + " no está disponible");
            }
            default -> {
                log.error("{}.{} falló con {}: {}", servicio, metodo, codigo, descripcion);
                yield new ErrorGateway(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
            }
        };
    }
}
