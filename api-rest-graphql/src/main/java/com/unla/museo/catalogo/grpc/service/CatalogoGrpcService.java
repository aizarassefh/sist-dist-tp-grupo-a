package com.unla.museo.catalogo.grpc.service;

import com.unla.museo.catalogo.entity.ArtistaEntity;
import com.unla.museo.catalogo.entity.ObraEntity;
import com.unla.museo.catalogo.grpc.ArtistasResponse;
import com.unla.museo.catalogo.grpc.BuscarObrasRequest;
import com.unla.museo.catalogo.grpc.CatalogoServiceGrpc;
import com.unla.museo.catalogo.grpc.Categoria;
import com.unla.museo.catalogo.grpc.CategoriasResponse;
import com.unla.museo.catalogo.grpc.ListarArtistasRequest;
import com.unla.museo.catalogo.grpc.ListarCategoriasRequest;
import com.unla.museo.catalogo.grpc.ObrasResponse;
import com.unla.museo.catalogo.grpc.ObtenerObrasRequest;
import com.unla.museo.catalogo.grpc.mapper.CatalogoGrpcMapper;
import com.unla.museo.catalogo.repository.ArtistaRepository;
import com.unla.museo.catalogo.repository.ObraRepository;
import com.unla.museo.comun.GrpcRespuestas;
import com.unla.museo.comun.Textos;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

// Transacción de solo lectura a nivel de clase: gRPC corre fuera de un request
// web, y sin transacción leer una relación LAZY tira LazyInitializationException.
@GrpcService
@Transactional(readOnly = true)
public class CatalogoGrpcService extends CatalogoServiceGrpc.CatalogoServiceImplBase {

    private final ObraRepository obraRepository;
    private final ArtistaRepository artistaRepository;

    public CatalogoGrpcService(ObraRepository obraRepository, ArtistaRepository artistaRepository) {
        this.obraRepository = obraRepository;
        this.artistaRepository = artistaRepository;
    }

    @Override
    public void buscarObras(BuscarObrasRequest pedido, StreamObserver<ObrasResponse> respuesta) {
        GrpcRespuestas.responder(respuesta, () -> buscarObras(pedido));
    }

    private ObrasResponse buscarObras(BuscarObrasRequest pedido) {
        List<String> movimientos = normalizarLista(pedido.getMovimientosList());
        List<String> tecnicas = normalizarLista(pedido.getTecnicasList());
        List<String> tematicas = normalizarLista(pedido.getTematicasList());

        ObrasResponse.Builder resultado = ObrasResponse.newBuilder();
        for (ObraEntity obra : obraRepository.listarConArtista()) {
            if (pedido.getSoloEnExhibicion() && !obra.isEnExhibicion()) {
                continue;
            }
            if (!coincideExacto(movimientos, obra.getEpoca())
                    || !coincideContiene(tecnicas, obra.getTecnica())
                    || !coincideExacto(tematicas, obra.getTematica())) {
                continue;
            }
            resultado.addObras(CatalogoGrpcMapper.aObra(obra));
        }
        return resultado.build();
    }

    @Override
    public void obtenerObras(ObtenerObrasRequest pedido, StreamObserver<ObrasResponse> respuesta) {
        GrpcRespuestas.responder(respuesta, () -> obtenerObras(pedido));
    }

    private ObrasResponse obtenerObras(ObtenerObrasRequest pedido) {
        if (pedido.getIdsList().isEmpty()) {
            throw Status.INVALID_ARGUMENT.withDescription("Hay que indicar al menos un id de obra")
                    .asRuntimeException();
        }
        Map<Long, ObraEntity> porId = obraRepository.buscarPorIdsConArtista(List.copyOf(pedido.getIdsList()))
                .stream().collect(Collectors.toMap(ObraEntity::getId, Function.identity()));

        // El orden del pedido se respeta porque el gateway arma recorridos y
        // comparaciones donde la posición importa.
        ObrasResponse.Builder resultado = ObrasResponse.newBuilder();
        for (Long id : pedido.getIdsList()) {
            ObraEntity obra = porId.get(id);
            if (obra == null) {
                throw Status.NOT_FOUND.withDescription("No existe la obra con id " + id).asRuntimeException();
            }
            resultado.addObras(CatalogoGrpcMapper.aObra(obra));
        }
        return resultado.build();
    }

    @Override
    public void listarArtistas(ListarArtistasRequest pedido, StreamObserver<ArtistasResponse> respuesta) {
        GrpcRespuestas.responder(respuesta, () -> listarArtistas(pedido));
    }

    private ArtistasResponse listarArtistas(ListarArtistasRequest pedido) {
        Map<Long, Integer> obrasPorArtista = new HashMap<>();
        for (ObraEntity obra : obraRepository.listarConArtista()) {
            obrasPorArtista.merge(obra.getArtista().getId(), 1, Integer::sum);
        }
        String movimiento = Textos.sinAcentos(pedido.getMovimiento());

        ArtistasResponse.Builder resultado = ArtistasResponse.newBuilder();
        for (ArtistaEntity artista : artistaRepository.findAll()) {
            if (!movimiento.isEmpty() && !movimiento.equals(Textos.sinAcentos(artista.getMovimiento()))) {
                continue;
            }
            resultado.addArtistas(CatalogoGrpcMapper.aArtista(artista, obrasPorArtista.getOrDefault(artista.getId(), 0)));
        }
        return resultado.build();
    }

    @Override
    public void listarCategorias(ListarCategoriasRequest pedido, StreamObserver<CategoriasResponse> respuesta) {
        GrpcRespuestas.responder(respuesta, () -> {
            List<ObraEntity> obras = obraRepository.listarConArtista();
            return CategoriasResponse.newBuilder()
                    .addAllMovimientos(contar(obras, ObraEntity::getEpoca))
                    .addAllTecnicas(contar(obras, ObraEntity::getTecnica))
                    .addAllTematicas(contar(obras, ObraEntity::getTematica))
                    .build();
        });
    }

    private static List<Categoria> contar(List<ObraEntity> obras, Function<ObraEntity, String> campo) {
        // Collator en español: un orden por código de carácter pondría "Óleo" después de "Temple".
        Map<String, Integer> conteos = new TreeMap<>(Collator.getInstance(Locale.of("es", "AR")));
        for (ObraEntity obra : obras) {
            String valor = campo.apply(obra);
            if (valor != null && !valor.isBlank()) {
                conteos.merge(valor.trim(), 1, Integer::sum);
            }
        }
        return conteos.entrySet().stream()
                .map(e -> Categoria.newBuilder().setNombre(e.getKey()).setCantidadObras(e.getValue()).build())
                .toList();
    }

    // Las entradas en blanco se descartan: una lista [""] equivale a "sin filtro".
    private static List<String> normalizarLista(List<String> valores) {
        return valores.stream().map(Textos::sinAcentos).filter(v -> !v.isEmpty()).toList();
    }

    private static boolean coincideExacto(List<String> criterios, String valor) {
        return criterios.isEmpty() || criterios.contains(Textos.sinAcentos(valor));
    }

    private static boolean coincideContiene(List<String> criterios, String valor) {
        if (criterios.isEmpty()) {
            return true;
        }
        String normalizado = Textos.sinAcentos(valor);
        return criterios.stream().anyMatch(normalizado::contains);
    }
}
