package com.unla.museo.eventos.service;

import com.unla.museo.eventos.util.TipoEvento;
import com.unla.museo.eventos.dto.EventoDTO;
import com.unla.museo.eventos.dto.EventoRequest;
import com.unla.museo.eventos.dto.FilaReporteEventoDTO;
import com.unla.museo.eventos.dto.PaginaEventosDTO;
import com.unla.museo.eventos.util.EstadoEvento;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;

public interface EventoService {

    PaginaEventosDTO listar(LocalDate desde, LocalDate hasta, TipoEvento tipo, Long curadorId,
                            EstadoEvento estado, Integer pagina, Integer tamanio, Authentication authentication);

    // La usan el reporte de asistencia (GraphQL) y su exportación a Excel, en
    // el módulo "reportes": una fila por evento, sin paginar, para que ambos
    // procesen siempre la misma lista. Igual que "listar", desde > hasta lanza
    // SolicitudInvalidaException.
    List<FilaReporteEventoDTO> obtenerFilasParaReporte(LocalDate desde, LocalDate hasta, TipoEvento tipo,
                                                        EstadoEvento estado);

    EventoDTO obtenerDetalle(Long id, Authentication authentication);

    EventoDTO crear(EventoRequest request);

    EventoDTO editar(Long id, EventoRequest request);

    EventoDTO modificarCupo(Long id, Integer nuevoCupo);

    void borrar(Long id);

    void inscribirse(Long eventoId, Authentication authentication);

    void desinscribirse(Long eventoId, Authentication authentication);
}
