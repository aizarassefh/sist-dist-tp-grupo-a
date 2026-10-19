package com.unla.museo.catalogo.service;

import com.unla.museo.catalogo.entity.FiltroObras;
import com.unla.museo.catalogo.dto.ArtistaDTO;
import com.unla.museo.catalogo.dto.ComentarioDTO;
import com.unla.museo.catalogo.dto.ObraDTO;

import java.util.List;
import java.util.Map;

public interface CatalogoService {

    List<ObraDTO> buscarObras(FiltroObras filtro, Integer pagina, Integer tamanio);

    ObraDTO buscarObraPorId(Long id);

    Map<Long, ArtistaDTO> obtenerArtistasPorIds(List<Long> ids);

    Map<Long, List<ComentarioDTO>> obtenerComentariosPorObraIds(List<Long> obraIds);
}
