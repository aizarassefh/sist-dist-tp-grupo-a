package com.unla.museo.mensajeria;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Novedad versión 1 tal como viaja por Kafka (docs/contrato-mensajeria.md). */
public record NovedadMensaje(
        UUID idMensaje,
        Integer versionEsquema,
        NovedadTipo tipo,
        Long idEvento,
        String tituloEvento,
        LocalDateTime fechaHoraEvento,
        Instant fechaOperacion,
        Long idUsuarioResponsable,
        String nombreUsuarioResponsable,
        EventoFoto eventoAnterior,
        EventoFoto eventoPosterior,
        List<String> camposModificados
) {

    public static final int VERSION_ESQUEMA = 1;
}
