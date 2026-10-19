package com.unla.museo.eventos.service;

import com.unla.museo.eventos.dto.FiltroFavoritoDTO;
import com.unla.museo.eventos.dto.FiltroFavoritoRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface FiltroFavoritoService {

    List<FiltroFavoritoDTO> listar(Authentication authentication);

    FiltroFavoritoDTO crear(FiltroFavoritoRequest request, Authentication authentication);

    FiltroFavoritoDTO editar(Long id, FiltroFavoritoRequest request, Authentication authentication);

    void borrar(Long id, Authentication authentication);
}
