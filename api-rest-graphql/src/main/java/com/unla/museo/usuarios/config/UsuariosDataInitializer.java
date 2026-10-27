package com.unla.museo.usuarios.config;

import com.unla.museo.catalogo.entity.ObraEntity;
import com.unla.museo.catalogo.repository.ObraRepository;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import com.unla.museo.usuarios.entity.InteraccionUsuario;
import com.unla.museo.usuarios.entity.InteraccionUsuario.TipoInteraccion;
import com.unla.museo.usuarios.entity.PreferenciasUsuario;
import com.unla.museo.usuarios.repository.InteraccionUsuarioRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Preferencias e interacciones de ejemplo para visitante@test.com, para que el
 * motor de recomendaciones tenga con qué trabajar en la demo. @Order(5): las
 * obras (4) tienen que existir para tomar sus ids. Solo completa lo que el
 * usuario todavía no tiene, así que es seguro correrlo en cada arranque.
 */
@Configuration
public class UsuariosDataInitializer {

    private static final String EMAIL = "visitante@test.com";

    @Bean
    @Order(5)
    @ConditionalOnProperty(name = "app.datos-de-ejemplo", havingValue = "true", matchIfMissing = true)
    CommandLineRunner initPreferenciasEInteraccionesDeEjemplo(
            @Qualifier("UsuarioSQLRepositoryImpl") UsuarioRepository usuarioRepository,
            InteraccionUsuarioRepository interaccionRepository,
            ObraRepository obraRepository,
            TransactionTemplate transaccion,
            Clock clock) {

        // Transacción propia: las listas de preferencias son LAZY y el runner
        // corre fuera de un request.
        return args -> transaccion.executeWithoutResult(estado -> {
            UsuarioEntity usuario = usuarioRepository.findByEmail(EMAIL)
                    .orElseThrow(() -> new IllegalStateException("Falta el usuario " + EMAIL));

            PreferenciasUsuario preferencias = usuario.getPreferencias();
            if (preferencias.getMovimientos().isEmpty() && preferencias.getTecnicas().isEmpty()
                    && preferencias.getTematicas().isEmpty()) {
                preferencias.getMovimientos().add("Impresionismo");
                preferencias.getTecnicas().add("Óleo");
                preferencias.getTematicas().add("Paisaje");
                usuarioRepository.save(usuario);
            }

            if (interaccionRepository.findByUsuarioIdOrderByFechaDescIdDesc(usuario.getId()).isEmpty()) {
                LocalDateTime ahora = LocalDateTime.now(clock);
                registrar(interaccionRepository, usuario, obraRepository, "La noche estrellada",
                        TipoInteraccion.VISTA, ahora.minusDays(2));
                registrar(interaccionRepository, usuario, obraRepository, "La Gioconda",
                        TipoInteraccion.INTERES, ahora.minusDays(1));
            }
        });
    }

    private static void registrar(InteraccionUsuarioRepository interaccionRepository, UsuarioEntity usuario,
                                  ObraRepository obraRepository, String tituloObra, TipoInteraccion tipo,
                                  LocalDateTime fecha) {
        ObraEntity obra = obraRepository.buscarPorTituloConArtista(tituloObra)
                .orElseThrow(() -> new IllegalStateException("Falta la obra " + tituloObra));
        InteraccionUsuario interaccion = new InteraccionUsuario();
        interaccion.setUsuario(usuario);
        interaccion.setObraId(obra.getId());
        interaccion.setTipo(tipo);
        interaccion.setFecha(fecha);
        interaccionRepository.save(interaccion);
    }
}
