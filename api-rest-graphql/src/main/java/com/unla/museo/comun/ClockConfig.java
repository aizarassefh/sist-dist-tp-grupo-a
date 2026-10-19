package com.unla.museo.comun;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * El "ahora" del negocio sale de este reloj (zona del museo), nunca de
 * LocalDateTime.now() directo, para que un test pueda fijarlo con
 * Clock.fixed(...) en vez de depender de la hora real de la máquina.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock relojDelSistema() {
        return Clock.system(ZoneId.of("America/Argentina/Buenos_Aires"));
    }
}
