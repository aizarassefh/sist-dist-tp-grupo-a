package com.unla.museo.eventos.config;

import com.unla.museo.eventos.entity.InscripcionEntity;
import com.unla.museo.eventos.repository.InscripcionRepository;
import com.unla.museo.eventos.util.TipoEvento;
import com.unla.museo.eventos.entity.EventoEntity;
import com.unla.museo.eventos.repository.EventoRepository;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Eventos de ejemplo para que el listado, el detalle y (en la fase
 * siguiente) el reporte tengan datos para mostrar en la demo. Solo corre si
 * la tabla de eventos está vacía, y solo con app.datos-de-ejemplo=true.
 *
 * Las fechas son offsets fijos en días respecto de LocalDateTime.now(clock)
 * (nunca Random), para que sean siempre relativas a "ahora" sin importar
 * cuándo se levante la aplicación.
 */
@Configuration
public class EventosDataInitializer {

    private record DatoEvento(int offsetDias, String titulo, String descripcion, TipoEvento tipo,
                               boolean primerCurador, int duracionMinutos, int cupoMaximo, int inscriptos) {
    }

    private static final List<DatoEvento> EVENTOS = List.of(
            new DatoEvento(-400, "Recorrido por el arte barroco",
                    "Visita comentada por la sala de arte europeo, con foco en el claroscuro y la composición barroca.",
                    TipoEvento.VISITA_GUIADA, true, 90, 20, 6),
            new DatoEvento(-120, "Taller de grabado y aguafuerte",
                    "Introducción a las técnicas de grabado calcográfico con materiales provistos por el museo.",
                    TipoEvento.TALLER, false, 120, 15, 4),
            new DatoEvento(-95, "Charla: la restauración de obras maestras",
                    "Una restauradora del museo cuenta cómo se recupera una obra dañada por el tiempo.",
                    TipoEvento.CHARLA, true, 60, 25, 8),
            new DatoEvento(-80, "Visita guiada: impresionismo y luz",
                    "Recorrido por la colección impresionista, con foco en el uso del color y la luz natural.",
                    TipoEvento.VISITA_GUIADA, false, 90, 18, 5),
            new DatoEvento(-60, "Taller de acuarela para principiantes",
                    "Taller práctico de iniciación a la acuarela, sin experiencia previa necesaria.",
                    TipoEvento.TALLER, true, 120, 12, 3),
            new DatoEvento(-45, "Charla: mujeres artistas del siglo XX",
                    "Un repaso por las artistas que marcaron el siglo XX y su lugar en la historia del arte.",
                    TipoEvento.CHARLA, false, 60, 30, 9),
            new DatoEvento(-30, "Recorrido nocturno por la colección permanente",
                    "Visita guiada fuera de horario habitual, con la colección permanente iluminada especialmente.",
                    TipoEvento.VISITA_GUIADA, true, 75, 10, 2),
            new DatoEvento(-15, "Taller de escultura en arcilla",
                    "Taller de modelado en arcilla inspirado en las esculturas de la colección del museo.",
                    TipoEvento.TALLER, false, 120, 20, 7),
            new DatoEvento(-5, "Charla: el arte precolombino en la región",
                    "Charla sobre las piezas precolombinas de la colección y su contexto histórico.",
                    TipoEvento.CHARLA, true, 60, 16, 0),
            new DatoEvento(3, "Visita guiada para familias",
                    "Recorrido pensado para familias con chicos, con actividades y preguntas a lo largo de la sala.",
                    TipoEvento.VISITA_GUIADA, false, 60, 10, 10),
            new DatoEvento(10, "Taller de fotografía analógica",
                    "Taller de introducción a la fotografía analógica en blanco y negro, con revelado incluido.",
                    TipoEvento.TALLER, true, 120, 22, 6),
            new DatoEvento(20, "Charla: crítica de arte contemporáneo",
                    "Charla abierta sobre cómo leer y discutir una obra de arte contemporáneo.",
                    TipoEvento.CHARLA, false, 60, 14, 0),
            new DatoEvento(35, "Recorrido por la sala de arte moderno",
                    "Visita guiada por la sala de arte moderno, con obras incorporadas en los últimos años.",
                    TipoEvento.VISITA_GUIADA, true, 90, 25, 5),
            new DatoEvento(50, "Taller de collage y técnica mixta",
                    "Taller de collage y técnica mixta a partir de materiales reciclados.",
                    TipoEvento.TALLER, false, 120, 18, 4),
            new DatoEvento(70, "Charla: conservación preventiva de pinturas",
                    "Charla sobre las condiciones de humedad, luz y temperatura que preservan una pintura.",
                    TipoEvento.CHARLA, true, 60, 20, 7),
            new DatoEvento(90, "Visita guiada: arte y arquitectura del museo",
                    "Recorrido que combina las obras exhibidas con la historia del edificio que las aloja.",
                    TipoEvento.VISITA_GUIADA, false, 90, 15, 3)
    );

    private static final List<String> EMAILS_VISITANTES = List.of(
            "visitante@test.com", "maria.gonzalez@test.com", "lucia.fernandez@test.com",
            "martin.rodriguez@test.com", "sofia.lopez@test.com", "nicolas.diaz@test.com",
            "valentina.martinez@test.com", "federico.sanchez@test.com", "camila.romero@test.com"
    );

    @Bean
    @Order(3)
    @ConditionalOnProperty(name = "app.datos-de-ejemplo", havingValue = "true", matchIfMissing = true)
    CommandLineRunner initEventosDeEjemplo(
            EventoRepository eventoRepository,
            InscripcionRepository inscripcionRepository,
            @Qualifier("UsuarioSQLRepositoryImpl") UsuarioRepository usuarioRepository,
            Clock clock) {

        return args -> {
            if (eventoRepository.count() > 0) {
                return;
            }

            UsuarioEntity curadorUno = usuarioRepository.findByEmail("curador@test.com")
                    .orElseThrow(() -> new IllegalStateException("Falta el usuario curador@test.com"));
            UsuarioEntity curadorDos = usuarioRepository.findByEmail("marta.gomez@test.com")
                    .orElseThrow(() -> new IllegalStateException("Falta el usuario marta.gomez@test.com"));
            UsuarioEntity admin = usuarioRepository.findByEmail("admin@test.com")
                    .orElseThrow(() -> new IllegalStateException("Falta el usuario admin@test.com"));

            List<UsuarioEntity> visitantes = EMAILS_VISITANTES.stream()
                    .map(email -> usuarioRepository.findByEmail(email)
                            .orElseThrow(() -> new IllegalStateException("Falta el usuario " + email)))
                    .toList();

            LocalDateTime ahora = LocalDateTime.now(clock);

            for (DatoEvento dato : EVENTOS) {
                EventoEntity eventoEntity = new EventoEntity();
                eventoEntity.setTitulo(dato.titulo());
                eventoEntity.setDescripcion(dato.descripcion());
                eventoEntity.setTipo(dato.tipo());
                eventoEntity.setFechaHora(ahora.toLocalDate().plusDays(dato.offsetDias()).atTime(horaDelEvento(dato.tipo())));
                eventoEntity.setDuracionMinutos(dato.duracionMinutos());
                eventoEntity.setCupoMaximo(dato.cupoMaximo());
                eventoEntity.setCurador(dato.primerCurador() ? curadorUno : curadorDos);
                eventoEntity = eventoRepository.save(eventoEntity);

                for (int i = 0; i < dato.inscriptos() && i < visitantes.size(); i++) {
                    inscribir(inscripcionRepository, eventoEntity, visitantes.get(i), ahora);
                }
                // El único evento con 10 inscriptos (cupo lleno) suma al admin
                // como décimo inscripto, porque el pool de visitantes tiene 9.
                if (dato.inscriptos() > visitantes.size()) {
                    inscribir(inscripcionRepository, eventoEntity, admin, ahora);
                }
            }
        };
    }

    // Horario fijo por tipo: si se tomara la hora del momento de la carga, una
    // base creada de madrugada tendría eventos a la 1 de la mañana.
    private static LocalTime horaDelEvento(TipoEvento tipo) {
        return switch (tipo) {
            case VISITA_GUIADA -> LocalTime.of(11, 0);
            case TALLER -> LocalTime.of(16, 0);
            case CHARLA -> LocalTime.of(18, 30);
        };
    }

    private void inscribir(InscripcionRepository inscripcionRepository, EventoEntity eventoEntity, UsuarioEntity usuario, LocalDateTime ahora) {
        InscripcionEntity inscripcionEntity = new InscripcionEntity();
        inscripcionEntity.setEvento(eventoEntity);
        inscripcionEntity.setUsuario(usuario);
        inscripcionEntity.setFechaInscripcion(ahora);
        inscripcionRepository.save(inscripcionEntity);
    }
}
