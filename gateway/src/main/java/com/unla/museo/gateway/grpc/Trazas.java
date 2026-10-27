package com.unla.museo.gateway.grpc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Llamadas gRPC de un pedido, en el orden en que terminaron. Hay una por
 * pedido (la crea el controller) y las llamadas paralelas escriben desde hilos
 * distintos, de ahí la lista concurrente.
 */
public final class Trazas {

    /** Forma JSON de cada traza: la consigna pide mostrar qué servicios se coordinaron. */
    public record Traza(String servicio, String metodo, long ms) {
    }

    private final List<Traza> llamadas = new CopyOnWriteArrayList<>();

    void registrar(String servicio, String metodo, long ms) {
        llamadas.add(new Traza(servicio, metodo, ms));
    }

    public List<Traza> comoLista() {
        return List.copyOf(llamadas);
    }
}
