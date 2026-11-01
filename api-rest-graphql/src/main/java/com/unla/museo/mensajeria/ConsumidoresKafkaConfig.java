package com.unla.museo.mensajeria;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Contenedores de los consumidores. Solo existen en un proceso que tenga
 * algún consumidor activo (app.kafka.consumidores no vacío).
 */
@Configuration
@ConditionalOnExpression("${app.kafka.enabled:false} and !'${app.kafka.consumidores:}'.isBlank()")
public class ConsumidoresKafkaConfig {

    private static final long ESPERA_MS = 2000;
    private static final long REINTENTOS = 3;

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> novedadesContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.topic}") String topic) {
        // El DLT tiene una sola partición: se fija la 0 en lugar de usar la del
        // mensaje original, que puede ser 1 o 2 y no existiría en el destino.
        DeadLetterPublishingRecoverer recuperador = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (registro, error) -> new TopicPartition(topic + ".DLT", 0));
        DefaultErrorHandler manejador = new DefaultErrorHandler(recuperador, new FixedBackOff(ESPERA_MS, REINTENTOS));
        // Reintentar un JSON inválido no lo arregla: directo al DLT.
        manejador.addNotRetryableExceptions(MensajeInvalidoException.class);
        return fabrica(consumerFactory, manejador);
    }

    // El consumidor del DLT no puede mandar sus propios errores al DLT: se
    // consumiría a sí mismo en un ciclo. Si falla (la base caída), reintenta
    // sin límite en lugar de descartar el mensaje fallido.
    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> fallidosContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        return fabrica(consumerFactory, new DefaultErrorHandler(new FixedBackOff(ESPERA_MS, FixedBackOff.UNLIMITED_ATTEMPTS)));
    }

    private static ConcurrentKafkaListenerContainerFactory<String, String> fabrica(
            ConsumerFactory<String, String> consumerFactory, DefaultErrorHandler manejador) {
        ConcurrentKafkaListenerContainerFactory<String, String> fabrica = new ConcurrentKafkaListenerContainerFactory<>();
        fabrica.setConsumerFactory(consumerFactory);
        // El offset se confirma después de cada registro, cuando la transacción
        // del consumidor ya hizo commit.
        fabrica.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        fabrica.setCommonErrorHandler(manejador);
        return fabrica;
    }
}
