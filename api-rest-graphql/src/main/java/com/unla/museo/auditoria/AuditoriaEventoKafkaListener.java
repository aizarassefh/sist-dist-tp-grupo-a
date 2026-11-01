package com.unla.museo.auditoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unla.museo.novedades.NovedadEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Consumidor independiente: persiste el historial sin depender del consumidor de novedades. */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class AuditoriaEventoKafkaListener {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaEventoKafkaListener.class);

    private final ObjectMapper objectMapper;
    private final AuditoriaEventoRepository repository;

    public AuditoriaEventoKafkaListener(ObjectMapper objectMapper, AuditoriaEventoRepository repository) {
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    @KafkaListener(
            topics = "${app.kafka.topic}",
            groupId = "${app.kafka.audit-consumer-group:museo-auditoria}",
            containerFactory = "novedadesKafkaListenerContainerFactory")
    @Transactional
    public void consumir(String payload) {
        try {
            NovedadEvento evento = objectMapper.readValue(payload, NovedadEvento.class);
            if (evento.messageId() == null) {
                throw new IllegalArgumentException("El mensaje de novedad no contiene messageId");
            }
            if (repository.existsById(evento.messageId())) {
                log.debug("Evento de auditoría duplicado ignorado messageId={}", evento.messageId());
                return;
            }
            repository.save(new AuditoriaEventoEntity(evento, payload));
            log.info("Actividad persistida en historial messageId={} eventId={} type={}",
                    evento.messageId(), evento.eventId(), evento.type());
        } catch (Exception exception) {
            // Se propaga: el error handler Kafka reintentará y, agotados los intentos,
            // conservará el mensaje en el topic de dead-letter.
            throw new IllegalArgumentException("No se pudo procesar la novedad para auditoría", exception);
        }
    }
}
