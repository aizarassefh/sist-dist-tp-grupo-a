package com.unla.museo.mensajeria;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Lee el outbox y publica en Kafka. Marca el mensaje como publicado recién
 * cuando el broker confirma (acks=all); si falla, lo deja pendiente y lo
 * reintenta en la vuelta siguiente. Entrega al menos una vez: un timeout puede
 * repetir un mensaje que sí llegó, y los consumidores lo absorben por idMensaje.
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "app.kafka", name = {"enabled", "publicador.enabled"}, havingValue = "true")
public class PublicadorDeNovedades {

    private static final long ESPERA_CONFIRMACION_SEGUNDOS = 5;

    private final MensajeSalienteRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;

    public PublicadorDeNovedades(MensajeSalienteRepository repository, KafkaTemplate<String, String> kafkaTemplate,
                                 Clock clock) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    // Sin @Transactional a propósito: no se retiene una transacción de base
    // mientras se espera al broker.
    @Scheduled(fixedDelayString = "${app.kafka.publicador.intervalo-ms:1000}")
    public void publicarPendientes() {
        // Si una clave falla, se salta el resto de sus mensajes en esta vuelta:
        // publicar la modificación antes que el alta rompería el orden por evento.
        Set<String> clavesConFallo = new HashSet<>();
        for (MensajeSalienteEntity mensaje : repository.findTop50ByFechaPublicacionIsNullOrderByFechaCreacionAscIdMensajeAsc()) {
            if (clavesConFallo.contains(mensaje.getClave())) {
                continue;
            }
            try {
                var envio = kafkaTemplate.send(mensaje.getTopic(), mensaje.getClave(), mensaje.getContenido())
                        .get(ESPERA_CONFIRMACION_SEGUNDOS, TimeUnit.SECONDS);
                mensaje.setFechaPublicacion(Instant.now(clock));
                repository.save(mensaje);
                log.info("Publicada la novedad {} (clave {}) en {}, partición {}, offset {}", mensaje.getIdMensaje(),
                        mensaje.getClave(), mensaje.getTopic(), envio.getRecordMetadata().partition(),
                        envio.getRecordMetadata().offset());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException | TimeoutException | RuntimeException e) {
                clavesConFallo.add(mensaje.getClave());
                registrarFallo(mensaje, e);
            }
        }
    }

    private void registrarFallo(MensajeSalienteEntity mensaje, Exception error) {
        log.warn("No se pudo publicar la novedad {}; se reintenta: {}", mensaje.getIdMensaje(), error.toString());
        mensaje.setIntentos(mensaje.getIntentos() + 1);
        String detalle = error.toString();
        mensaje.setUltimoError(detalle.length() > 1000 ? detalle.substring(0, 1000) : detalle);
        repository.save(mensaje);
    }
}
