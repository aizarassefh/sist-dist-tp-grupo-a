package com.unla.museo.mensajeria;

import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.seguridad.exception.UsuarioNoEncontradoException;
import com.unla.museo.seguridad.service.UsuarioActualService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lado productor: deja la novedad en el outbox dentro de la transacción de la
 * operación sobre el evento. Nunca habla con Kafka (eso lo hace el publicador),
 * así la operación del usuario no espera al broker ni se pierde si está caído.
 */
@Component
public class SalidaDeNovedades {

    private final MensajeSalienteRepository repository;
    private final FormatoNovedad formato;
    private final UsuarioActualService usuarioActualService;
    private final Clock clock;
    private final String topic;

    public SalidaDeNovedades(MensajeSalienteRepository repository, FormatoNovedad formato,
                             UsuarioActualService usuarioActualService, Clock clock,
                             @Value("${app.kafka.topic}") String topic) {
        this.repository = repository;
        this.formato = formato;
        this.usuarioActualService = usuarioActualService;
        this.clock = clock;
        this.topic = topic;
    }

    // MANDATORY: si alguien lo llama sin transacción, la novedad y la operación
    // podrían quedar desincronizadas, que es justo lo que el outbox evita.
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(NovedadTipo tipo, EventoFoto anterior, EventoFoto posterior) {
        List<String> campos = tipo == NovedadTipo.MODIFICACION ? camposModificados(anterior, posterior) : List.of();
        if (tipo == NovedadTipo.MODIFICACION && campos.isEmpty()) {
            return;
        }
        EventoFoto base = posterior != null ? posterior : anterior;
        UsuarioEntity responsable = responsableActual();
        UUID idMensaje = UUID.randomUUID();
        Instant ahora = Instant.now(clock);

        NovedadMensaje novedad = new NovedadMensaje(idMensaje, NovedadMensaje.VERSION_ESQUEMA, tipo,
                base.id(), base.titulo(), base.fechaHora(), ahora,
                responsable != null ? responsable.getId() : null,
                responsable != null ? responsable.getNombre() + " " + responsable.getApellido() : null,
                anterior, posterior, campos);

        MensajeSalienteEntity saliente = new MensajeSalienteEntity();
        saliente.setIdMensaje(idMensaje.toString());
        saliente.setTopic(topic);
        saliente.setClave(String.valueOf(base.id()));
        saliente.setContenido(formato.aJson(novedad));
        saliente.setFechaCreacion(ahora);
        repository.save(saliente);
    }

    // Los inscriptos quedan afuera a propósito: inscribirse no modifica el evento.
    // El curador se compara por id; su nombre cambia solo si cambia el id.
    private static List<String> camposModificados(EventoFoto a, EventoFoto p) {
        List<String> campos = new ArrayList<>();
        agregarSiCambia(campos, "titulo", a.titulo(), p.titulo());
        agregarSiCambia(campos, "descripcion", a.descripcion(), p.descripcion());
        agregarSiCambia(campos, "tipo", a.tipo(), p.tipo());
        agregarSiCambia(campos, "fechaHora", a.fechaHora(), p.fechaHora());
        agregarSiCambia(campos, "duracionMinutos", a.duracionMinutos(), p.duracionMinutos());
        agregarSiCambia(campos, "cupoMaximo", a.cupoMaximo(), p.cupoMaximo());
        agregarSiCambia(campos, "movimiento", a.movimiento(), p.movimiento());
        agregarSiCambia(campos, "idCurador", a.idCurador(), p.idCurador());
        return campos;
    }

    private static void agregarSiCambia(List<String> campos, String nombre, Object antes, Object despues) {
        if (!Objects.equals(antes, despues)) {
            campos.add(nombre);
        }
    }

    private UsuarioEntity responsableActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        try {
            return usuarioActualService.obtenerPorEmail(authentication.getName());
        } catch (UsuarioNoEncontradoException e) {
            return null;
        }
    }
}
