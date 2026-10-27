package com.unla.museo.comun;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Cierra una llamada unaria de los servicios gRPC. Una excepción lanzada desde
 * el handler NO se traduce sola: gRPC la convierte en UNKNOWN y pierde el
 * código (NOT_FOUND, INVALID_ARGUMENT). Por eso el estado se entrega por
 * onError, y lo inesperado sale como INTERNAL sin detalles internos.
 */
@Slf4j
public final class GrpcRespuestas {

    private GrpcRespuestas() {
    }

    public static <T> void responder(StreamObserver<T> respuesta, Supplier<T> accion) {
        T resultado;
        try {
            resultado = accion.get();
        } catch (StatusRuntimeException e) {
            respuesta.onError(e);
            return;
        } catch (RuntimeException e) {
            log.error("Error inesperado en un servicio gRPC", e);
            respuesta.onError(Status.INTERNAL.withDescription("Error interno del servidor").asRuntimeException());
            return;
        }
        respuesta.onNext(resultado);
        respuesta.onCompleted();
    }
}
