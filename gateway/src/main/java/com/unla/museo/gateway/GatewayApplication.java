package com.unla.museo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Cloud Gateway Application para Museo Virtual.
 * Punto de entrada único para el Hito 2 con orquestación gRPC.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("Spring Cloud Gateway iniciado");
        System.out.println("Conectado a gRPC en: spring-boot (9090) y NestJS (9091)");
    }
}
