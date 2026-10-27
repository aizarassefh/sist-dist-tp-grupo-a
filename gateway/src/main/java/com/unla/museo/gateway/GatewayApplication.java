package com.unla.museo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway del Museo Virtual: único punto de entrada de las funcionalidades
 * de la Entrega 2. Valida el JWT, orquesta llamadas gRPC a los servicios
 * internos y responde JSON. No accede a bases de datos ni tiene reglas de
 * negocio; las rutas de la Entrega 1 las reenvía al backend.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
