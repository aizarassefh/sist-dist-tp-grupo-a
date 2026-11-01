package com.unla.museo.mensajeria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unla.museo.comun.errores.ConflictoException;
import com.unla.museo.comun.errores.RecursoInexistenteException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Estado de la mensajería para la pantalla de administración, y reproceso de fallidos. */
@Service
public class MensajeriaService {

    // Los consumidores que existen: un consumidor nuevo se suma acá.
    static final List<String> CONSUMIDORES = List.of("notificaciones", "auditoria");
    private static final int MAXIMO_FALTANTES = 50;
    private static final int MAXIMO_FALLIDOS = 200;

    private final MensajeSalienteRepository salientes;
    private final MensajeProcesadoRepository procesados;
    private final MensajeFallidoRepository fallidos;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final boolean kafkaHabilitado;

    public MensajeriaService(MensajeSalienteRepository salientes, MensajeProcesadoRepository procesados,
                             MensajeFallidoRepository fallidos, KafkaTemplate<String, String> kafkaTemplate,
                             ObjectMapper objectMapper, @Value("${app.kafka.topic}") String topic,
                             @Value("${app.kafka.enabled:false}") boolean kafkaHabilitado) {
        this.salientes = salientes;
        this.procesados = procesados;
        this.fallidos = fallidos;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.kafkaHabilitado = kafkaHabilitado;
    }

    public ConsistenciaDTO consistencia() {
        long generados = salientes.count();
        long publicados = salientes.countByFechaPublicacionIsNotNull();
        long pendientes = generados - publicados;
        long fallidosPendientes = fallidos.countByEstado(EstadoFallido.PENDIENTE);

        List<ConsistenciaDTO.ConsumidorDTO> porConsumidor = CONSUMIDORES.stream()
                .map(consumidor -> new ConsistenciaDTO.ConsumidorDTO(consumidor,
                        procesados.contarPorConsumidor(consumidor),
                        salientes.idsPublicadosSinProcesar(consumidor, PageRequest.of(0, MAXIMO_FALTANTES))))
                .toList();

        boolean consistente = pendientes == 0 && fallidosPendientes == 0
                && porConsumidor.stream().allMatch(c -> c.faltantes().isEmpty());
        return new ConsistenciaDTO(generados, publicados, pendientes, porConsumidor, fallidosPendientes, consistente);
    }

    public List<FallidoDTO> listarFallidos(EstadoFallido estado) {
        PageRequest limite = PageRequest.of(0, MAXIMO_FALLIDOS);
        List<MensajeFallidoEntity> filas = estado != null
                ? fallidos.findByEstadoOrderByFechaRecepcionDescIdDesc(estado, limite)
                : fallidos.findAllByOrderByFechaRecepcionDescIdDesc(limite);
        return filas.stream().map(FallidoDTO::desde).toList();
    }

    public void reprocesar(Long id) {
        MensajeFallidoEntity fallido = fallidos.findById(id)
                .orElseThrow(() -> new RecursoInexistenteException("El mensaje fallido no existe"));
        if (fallido.getEstado() != EstadoFallido.PENDIENTE) {
            throw new ConflictoException("El mensaje fallido ya fue reprocesado");
        }
        if (!kafkaHabilitado) {
            throw new ConflictoException("Kafka está deshabilitado en este proceso");
        }
        publicarOriginal(fallido);

        // Se republica en el topic original, así que lo reciben todos los
        // consumidores: los fallidos del mismo mensaje quedan resueltos juntos.
        List<MensajeFallidoEntity> mismoMensaje = fallido.getIdMensaje() != null
                ? fallidos.findByIdMensajeAndEstado(fallido.getIdMensaje(), EstadoFallido.PENDIENTE)
                : List.of(fallido);
        mismoMensaje.forEach(f -> f.setEstado(EstadoFallido.REPROCESADO));
        fallidos.saveAll(mismoMensaje);
    }

    private void publicarOriginal(MensajeFallidoEntity fallido) {
        try {
            kafkaTemplate.send(topic, claveDe(fallido.getContenido()), fallido.getContenido()).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConflictoException("Se interrumpió la publicación en Kafka");
        } catch (ExecutionException | TimeoutException e) {
            throw new ConflictoException("No se pudo publicar en Kafka: " + e.getMessage());
        }
    }

    // La clave original (id del evento) no se guarda en mensajes_fallidos: se
    // recupera del contenido. Si no se puede leer, va sin clave.
    private String claveDe(String contenido) {
        try {
            JsonNode idEvento = objectMapper.readTree(contenido).get("idEvento");
            return idEvento != null && idEvento.isNumber() ? idEvento.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
