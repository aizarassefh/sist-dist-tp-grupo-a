package com.unla.museo.mensajeria;

import com.unla.museo.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los perfiles de consumidor arrancan sin servidor web ni datos de ejemplo.
 * "test" va después para reemplazar la base por H2 y el esquema por create-drop;
 * lo demás sale del perfil del consumidor. Con Kafka apagado (como en test) no
 * se levanta ningún listener: se prueba el contexto, no el consumo.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:museo-consumidor-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"consumidor-notificaciones", "test"})
class PerfilConsumidorSinWebTest {

    @Autowired
    private ApplicationContext contexto;
    @Autowired
    private Environment entorno;
    @Autowired
    @Qualifier("UsuarioSQLRepositoryImpl")
    private UsuarioRepository usuarioRepository;

    @Test
    void arrancaSinWebNiDatosDeEjemploYConSuConsumidor() {
        assertThat(contexto).isNotInstanceOf(WebApplicationContext.class);
        assertThat(entorno.getProperty("app.kafka.consumidores")).isEqualTo("notificaciones");
        assertThat(entorno.getProperty("app.kafka.publicador.enabled")).isEqualTo("false");
        assertThat(usuarioRepository.findByEmail("visitante@test.com")).isEmpty();
    }
}
