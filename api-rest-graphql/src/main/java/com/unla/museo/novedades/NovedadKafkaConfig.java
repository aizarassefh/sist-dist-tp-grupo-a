package com.unla.museo.novedades;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class NovedadKafkaConfig {
    private static final Logger log = LoggerFactory.getLogger(NovedadKafkaConfig.class);

    /** Hace explícito que los mensajes se reciben como JSON plano. */
    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
    ConcurrentKafkaListenerContainerFactory<String, String> novedadesKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // Tres reintentos con espera de 1 s. Si sigue fallando, se publica en
        // <topic>.DLT en lugar de descartar silenciosamente el acontecimiento.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
    NovedadPublisher kafkaNovedadPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                           @org.springframework.beans.factory.annotation.Value("${app.kafka.topic}") String topic,
                                           com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return novedad -> {
            try {
                String payload = objectMapper.writeValueAsString(novedad);
                kafkaTemplate.send(topic, novedad.messageId().toString(), payload)
                        .whenComplete((resultado, error) -> {
                            if (error != null) {
                                log.error("No se pudo publicar la novedad {} en Kafka topic={}",
                                        novedad.messageId(), topic, error);
                            } else {
                                SendResult<String, String> envio = resultado;
                                log.info("Novedad publicada en Kafka topic={} partition={} offset={} messageId={}",
                                        topic, envio.getRecordMetadata().partition(),
                                        envio.getRecordMetadata().offset(), novedad.messageId());
                            }
                        });
            } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
                throw new IllegalStateException("No se pudo serializar la novedad", exception);
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
    NovedadPublisher novedadPublisherDeshabilitado() {
        return novedad -> { };
    }
}
