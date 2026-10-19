package com.unla.museo.config;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Configuración específica para el servidor gRPC.
 * Verifica que los servicios gRPC estén registrados correctamente.
 */
@Slf4j
@Configuration
public class GrpcServerConfig {

    @EventListener(ApplicationReadyEvent.class)
    public void logGrpcServices() {
        log.info("=================================================");
        log.info("Servidor gRPC configurado en puerto: {}", GrpcConfig.GRPC_PORT);
        log.info("Los siguientes servicios deberían estar disponibles:");
        log.info("  - CatalogoService");
        log.info("  - EventosService");
        log.info("  - ComparacionService");
        log.info("  - ExploracionService");
        log.info("  - RecorridosService");
        log.info("  - RecomendacionesService");
        log.info("  - UsuariosService");
        log.info("=================================================");
    }
}
