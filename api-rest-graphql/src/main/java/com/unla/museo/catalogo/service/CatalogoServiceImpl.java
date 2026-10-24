package com.unla.museo.catalogo.service;

import com.unla.museo.catalogo.entity.ArtistaEntity;
import com.unla.museo.catalogo.entity.ComentarioEntity;
import com.unla.museo.catalogo.entity.FiltroObras;
import com.unla.museo.catalogo.entity.ObraEntity;
import com.unla.museo.catalogo.dto.ArtistaDTO;
import com.unla.museo.catalogo.dto.ComentarioDTO;
import com.unla.museo.catalogo.dto.ObraDTO;
import com.unla.museo.catalogo.repository.ArtistaRepository;
import com.unla.museo.catalogo.repository.ComentarioRepository;
import com.unla.museo.catalogo.repository.ObraRepository;
import com.unla.museo.comun.errores.SolicitudInvalidaException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CatalogoServiceImpl implements CatalogoService {

    private static final int TAMANIO_PAGINA_DEFECTO = 20;
    private static final int TAMANIO_PAGINA_MAXIMO = 100;

    private final ObraRepository obraRepository;
    private final ArtistaRepository artistaRepository;
    private final ComentarioRepository comentarioRepository;

    public CatalogoServiceImpl(ObraRepository obraRepository, ArtistaRepository artistaRepository,
                                ComentarioRepository comentarioRepository) {
        this.obraRepository = obraRepository;
        this.artistaRepository = artistaRepository;
        this.comentarioRepository = comentarioRepository;
    }

    @Override
    public List<ObraDTO> buscarObras(FiltroObras filtro, Integer pagina, Integer tamanio) {
        int paginaEfectiva = pagina != null ? pagina : 0;
        int tamanioEfectivo = tamanio != null ? tamanio : TAMANIO_PAGINA_DEFECTO;
        if (paginaEfectiva < 0) {
            throw new SolicitudInvalidaException("La página no puede ser negativa");
        }
        if (tamanioEfectivo < 1 || tamanioEfectivo > TAMANIO_PAGINA_MAXIMO) {
            throw new SolicitudInvalidaException("El tamaño de página debe estar entre 1 y " + TAMANIO_PAGINA_MAXIMO);
        }

        String palabraClave = normalizar(filtro != null ? filtro.getPalabraClave() : null);
        String epoca = normalizar(filtro != null ? filtro.getEpoca() : null);
        String tecnica = normalizar(filtro != null ? filtro.getTecnica() : null);
        String ubicacion = normalizar(filtro != null ? filtro.getUbicacion() : null);
        Boolean enExhibicion = filtro != null ? filtro.getEnExhibicion() : null;

        Pageable pageable = PageRequest.of(paginaEfectiva, tamanioEfectivo,
                Sort.by(Sort.Order.asc("titulo"), Sort.Order.asc("id")));
        return obraRepository.buscar(palabraClave, epoca, tecnica, ubicacion, enExhibicion, pageable)
                .getContent().stream()
                .map(this::aObraDTO)
                .toList();
    }

    @Override
    public ObraDTO buscarObraPorId(Long id) {
        return obraRepository.findById(id).map(this::aObraDTO).orElse(null);
    }

    @Override
    public Map<Long, ArtistaDTO> obtenerArtistasPorIds(List<Long> ids) {
        return artistaRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ArtistaEntity::getId,
                        a -> new ArtistaDTO(a.getNombre(), a.getBiografia())));
    }

    @Override
    public Map<Long, List<ComentarioDTO>> obtenerComentariosPorObraIds(List<Long> obraIds) {
        // groupingBy con LinkedHashMap y downstream toList: conserva, dentro
        // de cada obra, el orden por fecha que ya trae la consulta.
        return comentarioRepository.buscarPorObraIds(obraIds).stream()
                .collect(Collectors.groupingBy(c -> c.getObra().getId(), LinkedHashMap::new,
                        Collectors.mapping(this::aComentarioDTO, Collectors.toList())));
    }

    private ObraDTO aObraDTO(ObraEntity o) {
        return new ObraDTO(o.getId(), o.getTitulo(), o.getArtista().getId(), o.getImagenUrl(), o.getAnioCreacion(),
                o.getTecnica(), o.getDimensiones(), o.getEpoca(), o.getDescripcion(), o.getUbicacion(),
                o.isEnExhibicion());
    }

    private ComentarioDTO aComentarioDTO(ComentarioEntity c) {
        return new ComentarioDTO(c.getUsuario().getNombre() + " " + c.getUsuario().getApellido(), c.getTexto(),
                c.getFecha().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String v = valor.trim().toLowerCase(Locale.ROOT);
        return v.isBlank() ? null : v;
    }
}
