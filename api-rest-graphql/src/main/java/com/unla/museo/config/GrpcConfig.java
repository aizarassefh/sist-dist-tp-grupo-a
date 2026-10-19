package com.unla.museo.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuración de gRPC para el servicio Spring Boot.
 *
 * NOTA: Cuando se usa grpc-server-spring-boot-starter, el servidor gRPC
 * se configura automáticamente a través de application.properties.
 * Los servicios con @GrpcService se registran automáticamente.
 *
 * Ver: application.properties -> grpc.server.port=9090
 */
@Configuration
public class GrpcConfig {

    /**
     * Puerto del servidor gRPC.
     * Se utiliza el puerto 9090 para no interferir con el puerto HTTP 8000.
     * Configurado en application.properties
     */
    public static final int GRPC_PORT = 9090;
}
