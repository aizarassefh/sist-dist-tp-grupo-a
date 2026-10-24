package com.unla.museo.eventos.dto;

import java.util.List;

public record PaginaEventosDTO(List<EventoDTO> items, int pagina, int tamanio, long total) {
}
