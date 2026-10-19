package com.unla.museo.catalogo.resolver;

import com.unla.museo.catalogo.entity.FiltroObras;
import com.unla.museo.catalogo.dto.ArtistaDTO;
import com.unla.museo.catalogo.dto.ComentarioDTO;
import com.unla.museo.catalogo.dto.ObraDTO;
import com.unla.museo.catalogo.service.CatalogoService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class CatalogoResolver {

    private final CatalogoService catalogoService;

    public CatalogoResolver(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @QueryMapping
    public List<ObraDTO> obras(@Argument FiltroObras filtro, @Argument Integer pagina, @Argument Integer tamanio) {
        return catalogoService.buscarObras(filtro, pagina, tamanio);
    }

    // El argumento llega como String (así lo entrega el scalar ID de
    // GraphQL Java) y se parsea a mano en vez de depender de una conversión
    // automática no verificada. Un id no numérico no puede existir: se trata
    // igual que un id inexistente y devuelve null.
    @QueryMapping
    public ObraDTO obra(@Argument String id) {
        try {
            return catalogoService.buscarObraPorId(Long.valueOf(id));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // typeName = "Obra" es necesario porque el DTO fuente se llama ObraDTO,
    // no Obra: sin esto Spring no asocia el batch loader con el tipo GraphQL
    // y el campo queda siempre en null (aunque la query no falle).
    // Una sola consulta por lote de obras (findAllById), no una por obra.
    @BatchMapping(typeName = "Obra")
    public Map<ObraDTO, ArtistaDTO> artista(List<ObraDTO> obras) {
        List<Long> ids = obras.stream().map(ObraDTO::artistaId).distinct().toList();
        Map<Long, ArtistaDTO> artistasPorId = catalogoService.obtenerArtistasPorIds(ids);
        return obras.stream().collect(Collectors.toMap(o -> o, o -> artistasPorId.get(o.artistaId())));
    }

    // Ídem: una sola consulta por lote de obras, con el autor ya resuelto
    // (join fetch en ComentarioRepository) para no sumar otro N+1.
    @BatchMapping(typeName = "Obra")
    public Map<ObraDTO, List<ComentarioDTO>> comentarios(List<ObraDTO> obras) {
        List<Long> ids = obras.stream().map(ObraDTO::id).toList();
        Map<Long, List<ComentarioDTO>> comentariosPorObra = catalogoService.obtenerComentariosPorObraIds(ids);
        return obras.stream().collect(Collectors.toMap(o -> o, o -> comentariosPorObra.getOrDefault(o.id(), List.of())));
    }
}
