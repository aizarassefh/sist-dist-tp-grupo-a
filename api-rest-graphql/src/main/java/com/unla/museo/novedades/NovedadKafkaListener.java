package com.unla.museo.novedades;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class NovedadKafkaListener {
    private static final Logger log = LoggerFactory.getLogger(NovedadKafkaListener.class);
    private final ObjectMapper objectMapper;
    private final NovedadRepository repository;
    private final NovedadStream stream;

    public NovedadKafkaListener(ObjectMapper objectMapper, NovedadRepository repository, NovedadStream stream) {
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.stream = stream;
    }

    @PostConstruct
    void informarConfiguracionActiva() {
        log.info("Listener de novedades Kafka activo; esperando mensajes del topic configurado");
    }

    @KafkaListener(
            topics = "${app.kafka.topic}",
            containerFactory = "novedadesKafkaListenerContainerFactory")
    @Transactional
    public void consumir(String payload) {
        try {
            NovedadEvento evento = objectMapper.readValue(payload, NovedadEvento.class);
            if (!repository.existsById(evento.messageId())) {
                NovedadEntity guardada = repository.save(new NovedadEntity(evento));
                stream.publicar(guardada);
                log.info("Novedad consumida desde Kafka messageId={} eventId={} type={}",
                        evento.messageId(), evento.eventId(), evento.type());
            } else {
                log.debug("Novedad Kafka duplicada ignorada messageId={}", evento.messageId());
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Novedad Kafka inválida", exception);
        }
    }
}
