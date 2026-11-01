package com.unla.museo.mensajeria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;

/** Guarda en mensajes_fallidos lo que llegó al DLT, para verlo y reprocesarlo. */
@Slf4j
@Component
@ConditionalOnExpression("${app.kafka.enabled:false} and !'${app.kafka.consumidores:}'.isBlank()")
public class FallidosConsumidor {

    static final String CONSUMIDOR = "fallidos";
    private static final String PREFIJO_GRUPO = "museo-";

    private final ProcesadorIdempotente procesador;
    private final MensajeFallidoRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public FallidosConsumidor(ProcesadorIdempotente procesador, MensajeFallidoRepository repository,
                              ObjectMapper objectMapper, Clock clock) {
        this.procesador = procesador;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @KafkaListener(topics = "${app.kafka.topic}.DLT", groupId = "museo-fallidos", containerFactory = "fallidosContainerFactory")
    public void consumir(ConsumerRecord<String, String> registro) {
        // La posición en el DLT identifica la entrega: una repetición tras una
        // caída entre el commit y el ack no duplica la fila.
        String posicion = registro.topic() + ":" + registro.partition() + ":" + registro.offset();
        procesador.procesar(CONSUMIDOR, posicion, () -> guardar(registro));
    }

    private void guardar(ConsumerRecord<String, String> registro) {
        MensajeFallidoEntity fallido = new MensajeFallidoEntity();
        fallido.setContenido(registro.value());
        fallido.setIdMensaje(idMensajeDe(registro.value()));
        fallido.setConsumidor(consumidorDeOrigen(registro));
        String error = causa(encabezado(registro, KafkaHeaders.DLT_EXCEPTION_MESSAGE));
        fallido.setError(error != null && error.length() > 2000 ? error.substring(0, 2000) : error);
        fallido.setFechaRecepcion(Instant.now(clock));
        fallido.setEstado(EstadoFallido.PENDIENTE);
        repository.save(fallido);
    }

    // Spring Kafka envuelve la causa en "Listener method '...' threw exception; <causa>":
    // en la pantalla de mensajería interesa la causa, no la firma del método.
    private static String causa(String error) {
        if (error == null) {
            return null;
        }
        int corte = error.indexOf(" threw exception; ");
        return corte >= 0 ? error.substring(corte + " threw exception; ".length()) : error;
    }

    // "museo-notificaciones" -> "notificaciones", el nombre que usa mensajes_procesados.
    private static String consumidorDeOrigen(ConsumerRecord<String, String> registro) {
        String grupo = encabezado(registro, KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP);
        if (grupo == null) {
            return null;
        }
        return grupo.startsWith(PREFIJO_GRUPO) ? grupo.substring(PREFIJO_GRUPO.length()) : grupo;
    }

    private String idMensajeDe(String contenido) {
        if (contenido == null) {
            return null;
        }
        try {
            JsonNode id = objectMapper.readTree(contenido).get("idMensaje");
            return id != null && id.isTextual() && id.asText().length() <= 36 ? id.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String encabezado(ConsumerRecord<String, String> registro, String nombre) {
        Header header = registro.headers().lastHeader(nombre);
        return header != null && header.value() != null ? new String(header.value(), StandardCharsets.UTF_8) : null;
    }
}
