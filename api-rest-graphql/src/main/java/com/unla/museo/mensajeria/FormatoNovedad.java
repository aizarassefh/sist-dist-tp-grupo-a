package com.unla.museo.mensajeria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Un único lugar donde se escribe y se valida el JSON de la novedad. */
@Component
public class FormatoNovedad {

    private final ObjectMapper objectMapper;

    public FormatoNovedad(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String aJson(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar a JSON", e);
        }
    }

    public NovedadMensaje leer(String json) {
        if (json == null || json.isBlank()) {
            throw new MensajeInvalidoException("El mensaje está vacío");
        }
        NovedadMensaje novedad;
        try {
            novedad = objectMapper.readValue(json, NovedadMensaje.class);
        } catch (JsonProcessingException e) {
            throw new MensajeInvalidoException("El mensaje no es un JSON de novedad válido: " + e.getOriginalMessage(), e);
        }
        validar(novedad);
        return novedad;
    }

    private static void validar(NovedadMensaje n) {
        if (n == null) {
            throw new MensajeInvalidoException("El mensaje está vacío");
        }
        if (n.versionEsquema() == null || n.versionEsquema() != NovedadMensaje.VERSION_ESQUEMA) {
            throw new MensajeInvalidoException("Versión de esquema no soportada: " + n.versionEsquema());
        }
        exigir(n.idMensaje(), "idMensaje");
        exigir(n.tipo(), "tipo");
        exigir(n.idEvento(), "idEvento");
        exigir(n.tituloEvento(), "tituloEvento");
        exigir(n.fechaHoraEvento(), "fechaHoraEvento");
        exigir(n.fechaOperacion(), "fechaOperacion");
        exigir(n.camposModificados(), "camposModificados");
        // Cada tipo necesita la foto de la que saca sus destinatarios.
        switch (n.tipo()) {
            case ALTA -> exigir(n.eventoPosterior(), "eventoPosterior");
            case MODIFICACION -> {
                exigir(n.eventoAnterior(), "eventoAnterior");
                exigir(n.eventoPosterior(), "eventoPosterior");
            }
            case CANCELACION -> exigir(n.eventoAnterior(), "eventoAnterior");
        }
    }

    private static void exigir(Object valor, String campo) {
        if (valor == null) {
            throw new MensajeInvalidoException("Falta el campo obligatorio '" + campo + "'");
        }
    }
}
