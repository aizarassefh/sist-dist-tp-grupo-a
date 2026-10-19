package com.unla.museo.eventos.service.impl;

import com.unla.museo.comun.errores.RecursoInexistenteException;
import com.unla.museo.eventos.entity.FiltroFavoritoEntity;
import com.unla.museo.eventos.repository.FiltroFavoritoRepository;
import com.unla.museo.eventos.service.FiltroFavoritoService;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.eventos.dto.FiltroFavoritoDTO;
import com.unla.museo.eventos.dto.FiltroFavoritoRequest;
import com.unla.museo.eventos.dto.FiltrosDTO;
import com.unla.museo.seguridad.service.UsuarioActualService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FiltroFavoritoServiceImpl implements FiltroFavoritoService {

    private final FiltroFavoritoRepository filtroFavoritoRepository;
    private final UsuarioActualService usuarioActualService;

    public FiltroFavoritoServiceImpl(FiltroFavoritoRepository filtroFavoritoRepository,
                                      UsuarioActualService usuarioActualService) {
        this.filtroFavoritoRepository = filtroFavoritoRepository;
        this.usuarioActualService = usuarioActualService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FiltroFavoritoDTO> listar(Authentication authentication) {
        UsuarioEntity usuario = usuarioActualService.obtenerPorEmail(authentication.getName());
        return filtroFavoritoRepository.findByUsuarioIdOrderByIdAsc(usuario.getId()).stream()
                .map(this::mapear)
                .toList();
    }

    @Override
    public FiltroFavoritoDTO crear(FiltroFavoritoRequest request, Authentication authentication) {
        UsuarioEntity usuario = usuarioActualService.obtenerPorEmail(authentication.getName());
        FiltroFavoritoEntity entidad = new FiltroFavoritoEntity();
        entidad.setUsuario(usuario);
        aplicar(entidad, request);
        return mapear(filtroFavoritoRepository.save(entidad));
    }

    @Override
    public FiltroFavoritoDTO editar(Long id, FiltroFavoritoRequest request, Authentication authentication) {
        FiltroFavoritoEntity entidad = buscarPropio(id, authentication);
        aplicar(entidad, request);
        return mapear(filtroFavoritoRepository.save(entidad));
    }

    @Override
    public void borrar(Long id, Authentication authentication) {
        FiltroFavoritoEntity entidad = buscarPropio(id, authentication);
        filtroFavoritoRepository.delete(entidad);
    }

    private FiltroFavoritoEntity buscarPropio(Long id, Authentication authentication) {
        UsuarioEntity usuario = usuarioActualService.obtenerPorEmail(authentication.getName());
        return filtroFavoritoRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new RecursoInexistenteException("El filtro favorito no existe"));
    }

    private void aplicar(FiltroFavoritoEntity entidad, FiltroFavoritoRequest request) {
        entidad.setNombre(request.getNombre());
        entidad.setDescripcion(request.getDescripcion());
        FiltrosDTO filtros = request.getFiltros();
        entidad.setDesde(filtros != null ? filtros.desde() : null);
        entidad.setHasta(filtros != null ? filtros.hasta() : null);
        entidad.setTipo(filtros != null ? filtros.tipo() : null);
        entidad.setCuradorId(filtros != null ? filtros.curadorId() : null);
        entidad.setEstado(filtros != null ? filtros.estado() : null);
    }

    private FiltroFavoritoDTO mapear(FiltroFavoritoEntity entidad) {
        FiltrosDTO filtros = new FiltrosDTO(entidad.getDesde(), entidad.getHasta(), entidad.getTipo(),
                entidad.getCuradorId(), entidad.getEstado());
        return new FiltroFavoritoDTO(entidad.getId(), entidad.getNombre(), entidad.getDescripcion(), filtros);
    }
}
