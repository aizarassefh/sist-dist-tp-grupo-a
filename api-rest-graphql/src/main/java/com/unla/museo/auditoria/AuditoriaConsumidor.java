package com.unla.museo.auditoria;

import com.unla.museo.mensajeria.FormatoNovedad;
import com.unla.museo.mensajeria.NovedadMensaje;
import com.unla.museo.mensajeria.ProcesadorIdempotente;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/** Registra cada novedad en el historial, con la foto anterior y la posterior. */
@Component
@ConditionalOnExpression("${app.kafka.enabled:false} and '${app.kafka.consumidores:}'.contains('auditoria')")
public class AuditoriaConsumidor {

    static final String CONSUMIDOR = "auditoria";

    private final FormatoNovedad formato;
    private final ProcesadorIdempotente procesador;
    private final HistorialActividadRepository repository;
    private final Clock clock;

    public AuditoriaConsumidor(FormatoNovedad formato, ProcesadorIdempotente procesador,
                               HistorialActividadRepository repository, Clock clock) {
        this.formato = formato;
        this.procesador = procesador;
        this.repository = repository;
        this.clock = clock;
    }

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "museo-auditoria", containerFactory = "novedadesContainerFactory")
    public void consumir(String contenido) {
        NovedadMensaje novedad = formato.leer(contenido);
        procesador.procesar(CONSUMIDOR, novedad.idMensaje().toString(), () -> registrar(novedad));
    }

    private void registrar(NovedadMensaje novedad) {
        HistorialActividadEntity h = new HistorialActividadEntity();
        h.setIdMensaje(novedad.idMensaje().toString());
        h.setTipo(novedad.tipo().name());
        h.setIdEvento(novedad.idEvento());
        h.setTituloEvento(novedad.tituloEvento());
        h.setFechaHoraEvento(novedad.fechaHoraEvento());
        h.setFechaOperacion(novedad.fechaOperacion());
        h.setIdUsuarioResponsable(novedad.idUsuarioResponsable());
        h.setNombreUsuarioResponsable(novedad.nombreUsuarioResponsable());
        h.setCamposModificados(String.join(",", novedad.camposModificados()));
        h.setDatosAnteriores(novedad.eventoAnterior() != null ? formato.aJson(novedad.eventoAnterior()) : null);
        h.setDatosPosteriores(novedad.eventoPosterior() != null ? formato.aJson(novedad.eventoPosterior()) : null);
        h.setFechaProcesamiento(Instant.now(clock));
        repository.save(h);
    }
}
