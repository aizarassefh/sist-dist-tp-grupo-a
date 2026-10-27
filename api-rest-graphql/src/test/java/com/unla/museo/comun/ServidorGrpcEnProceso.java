package com.unla.museo.comun;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Servidor gRPC en memoria con un solo servicio, para probarlo con un stub real
 * sin abrir puertos (el perfil test deja grpc.server.port=-1). Se registra el
 * bean de Spring, así las llamadas pasan por su proxy transaccional.
 */
public class ServidorGrpcEnProceso implements AutoCloseable {

    private final Server servidor;
    private final ManagedChannel canal;

    public ServidorGrpcEnProceso(BindableService servicio) throws IOException {
        String nombre = "test-" + UUID.randomUUID();
        // directExecutor: el servicio corre en el hilo del test, sin sorpresas de concurrencia.
        servidor = InProcessServerBuilder.forName(nombre).directExecutor().addService(servicio).build().start();
        canal = InProcessChannelBuilder.forName(nombre).directExecutor().build();
    }

    public ManagedChannel canal() {
        return canal;
    }

    @Override
    public void close() throws InterruptedException {
        canal.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        servidor.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
}
