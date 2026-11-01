package com.unla.museo.mensajeria;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;

/**
 * Ejecuta el efecto de un consumidor una sola vez por mensaje: registra
 * (consumidor, idMensaje) en mensajes_procesados dentro de la misma transacción
 * que el efecto. Si el efecto falla, el registro también se deshace y el
 * reintento vuelve a intentarlo completo.
 */
@Slf4j
@Component
public class ProcesadorIdempotente {

    private final TransactionTemplate transaccion;
    private final MensajeProcesadoRepository procesados;
    private final Clock clock;

    public ProcesadorIdempotente(TransactionTemplate transaccion, MensajeProcesadoRepository procesados, Clock clock) {
        this.transaccion = transaccion;
        this.procesados = procesados;
        this.clock = clock;
    }

    /** @return true si ejecutó el efecto; false si el mensaje ya estaba procesado. */
    public boolean procesar(String consumidor, String idMensaje, Runnable efecto) {
        MensajeProcesadoId id = new MensajeProcesadoId(consumidor, idMensaje);
        boolean[] registrado = {false};
        try {
            return Boolean.TRUE.equals(transaccion.execute(estado -> {
                if (procesados.existsById(id)) {
                    log.info("{}: el mensaje {} ya estaba procesado; se ignora (duplicado)", consumidor, idMensaje);
                    return false;
                }
                procesados.saveAndFlush(new MensajeProcesadoEntity(id, Instant.now(clock)));
                registrado[0] = true;
                efecto.run();
                log.info("{}: procesado el mensaje {}", consumidor, idMensaje);
                return true;
            }));
        } catch (DataIntegrityViolationException e) {
            // Otro hilo registró el mismo mensaje entre el existsById y el insert
            // (la carrera que existsById solo no cubre). Si el registro ya estaba
            // hecho, la violación viene del efecto y es un error real.
            if (registrado[0]) {
                throw e;
            }
            log.info("El mensaje {} ya fue procesado por {} en paralelo; se ignora", idMensaje, consumidor);
            return false;
        }
    }
}
