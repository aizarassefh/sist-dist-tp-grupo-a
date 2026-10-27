package com.unla.museo.catalogo;

import com.unla.museo.catalogo.grpc.Artista;
import com.unla.museo.catalogo.grpc.BuscarObrasRequest;
import com.unla.museo.catalogo.grpc.CatalogoServiceGrpc;
import com.unla.museo.catalogo.grpc.Categoria;
import com.unla.museo.catalogo.grpc.CategoriasResponse;
import com.unla.museo.catalogo.grpc.ListarArtistasRequest;
import com.unla.museo.catalogo.grpc.ListarCategoriasRequest;
import com.unla.museo.catalogo.grpc.Obra;
import com.unla.museo.catalogo.grpc.ObtenerObrasRequest;
import com.unla.museo.catalogo.grpc.service.CatalogoGrpcService;
import com.unla.museo.comun.ServidorGrpcEnProceso;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.text.Collator;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Llama a CatalogoService por un servidor gRPC en memoria. Usa las 9 obras
 * originales de CatalogoDataInitializer (el perfil test no carga las
 * ampliadas, ver CatalogoDatosAmpliadosTest); ningún test las modifica.
 */
@SpringBootTest
@ActiveProfiles("test")
class CatalogoGrpcServiceTest {

    @Autowired
    private CatalogoGrpcService servicio;

    private ServidorGrpcEnProceso servidor;
    private CatalogoServiceGrpc.CatalogoServiceBlockingStub stub;

    @BeforeEach
    void levantarServidor() throws Exception {
        servidor = new ServidorGrpcEnProceso(servicio);
        stub = CatalogoServiceGrpc.newBlockingStub(servidor.canal());
    }

    @AfterEach
    void bajarServidor() throws Exception {
        servidor.close();
    }

    private List<String> titulos(BuscarObrasRequest pedido) {
        return stub.buscarObras(pedido).getObrasList().stream().map(Obra::getTitulo).toList();
    }

    @Test
    void buscarObrasCombinaCriteriosIgnorandoMayusculasYAcentos() {
        // "oleo" sin acento y "POSTIMPRESIONISMO" en mayúsculas igual encuentran "Óleo sobre lienzo" / "Postimpresionismo".
        List<String> titulos = titulos(BuscarObrasRequest.newBuilder()
                .addMovimientos("POSTIMPRESIONISMO").addTecnicas("oleo").addTematicas("paisaje").build());

        assertThat(titulos).containsExactly("La noche estrellada");
        // Con acento en el criterio y sin acento en el dato (y al revés) también coincide.
        assertThat(titulos(BuscarObrasRequest.newBuilder().addTematicas("mitologia").build()))
                .contains("El nacimiento de Venus");
        assertThat(titulos(BuscarObrasRequest.newBuilder().addTematicas("MITOLOGÍA").build()))
                .contains("El nacimiento de Venus");
    }

    @Test
    void buscarObrasUsaORDentroDeUnaListaYANDEntreListas() {
        List<String> renacimientoOBarroco = titulos(BuscarObrasRequest.newBuilder()
                .addMovimientos("Renacimiento").addMovimientos("barroco").build());
        assertThat(renacimientoOBarroco).contains("La Gioconda", "El nacimiento de Venus",
                "Las meninas", "La joven de la perla")
                .doesNotContain("Guernica", "El grito");

        // AND con la técnica: de esos dos movimientos, solo lo pintado al temple ("contiene", no igualdad).
        List<String> alTemple = titulos(BuscarObrasRequest.newBuilder()
                .addMovimientos("Renacimiento").addMovimientos("Barroco").addTecnicas("TEMPLE").build());
        assertThat(alTemple).containsExactly("El nacimiento de Venus");
    }

    @Test
    void buscarObrasSinCriteriosDevuelveTodasYSoloEnExhibicionFiltra() {
        List<String> todas = titulos(BuscarObrasRequest.getDefaultInstance());
        assertThat(todas).contains("Guernica", "La Gioconda").hasSizeGreaterThanOrEqualTo(9);

        // Una lista con un string vacío equivale a no filtrar.
        assertThat(titulos(BuscarObrasRequest.newBuilder().addMovimientos("").build())).containsAll(todas);

        List<String> enExhibicion = titulos(BuscarObrasRequest.newBuilder().setSoloEnExhibicion(true).build());
        assertThat(enExhibicion).contains("La Gioconda").doesNotContain("Guernica", "El grito");
    }

    @Test
    void buscarObrasDevuelveElMovimientoYElArtistaCompletos() {
        Obra obra = stub.buscarObras(BuscarObrasRequest.newBuilder().addTecnicas("pan de oro").build())
                .getObras(0);

        assertThat(obra.getTitulo()).isEqualTo("El beso");
        assertThat(obra.getMovimiento()).isEqualTo("Modernismo");
        assertThat(obra.getTematica()).isEqualTo("Escena cotidiana");
        assertThat(obra.getEnExhibicion()).isTrue();
        assertThat(obra.getUbicacion()).isEqualTo("Sala de Arte Moderno");
        assertThat(obra.getArtista().getNombre()).isEqualTo("Gustav Klimt");
        assertThat(obra.getArtista().getNacionalidad()).isEqualTo("Austríaca");
        assertThat(obra.getArtista().getAnioNacimiento()).isEqualTo(1862);
        assertThat(obra.getArtista().getAnioFallecimiento()).isEqualTo(1918);
    }

    @Test
    void obtenerObrasRespetaElOrdenPedido() {
        List<Long> ids = stub.buscarObras(BuscarObrasRequest.getDefaultInstance()).getObrasList().stream()
                .filter(o -> List.of("Guernica", "La Gioconda", "El beso").contains(o.getTitulo()))
                .sorted((a, b) -> a.getTitulo().compareTo(b.getTitulo()))
                .map(Obra::getId).toList();

        // El orden pedido queda distinto del de inserción (la Gioconda se cargó primero).
        List<Long> pedidos = List.of(ids.get(2), ids.get(0), ids.get(1));
        List<Long> devueltos = stub.obtenerObras(ObtenerObrasRequest.newBuilder().addAllIds(pedidos).build())
                .getObrasList().stream().map(Obra::getId).toList();

        assertThat(devueltos).containsExactlyElementsOf(pedidos);
    }

    @Test
    void obtenerObrasConUnIdInexistenteDevuelveNotFound() {
        long idValido = stub.buscarObras(BuscarObrasRequest.getDefaultInstance()).getObras(0).getId();

        assertThatThrownBy(() -> stub.obtenerObras(
                ObtenerObrasRequest.newBuilder().addIds(idValido).addIds(999_999L).build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class, e -> {
                    assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
                    assertThat(e.getStatus().getDescription()).contains("999999");
                });
    }

    @Test
    void obtenerObrasSinIdsEsInvalid() {
        assertThatThrownBy(() -> stub.obtenerObras(ObtenerObrasRequest.getDefaultInstance()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
    }

    @Test
    void listarArtistasInformaCantidadDeObrasYFiltraPorMovimiento() {
        List<Artista> todos = stub.listarArtistas(ListarArtistasRequest.getDefaultInstance()).getArtistasList();
        Artista picasso = todos.stream().filter(a -> a.getNombre().equals("Pablo Picasso")).findFirst().orElseThrow();
        assertThat(picasso.getCantidadObras()).isGreaterThanOrEqualTo(1);

        List<Artista> barrocos = stub.listarArtistas(
                ListarArtistasRequest.newBuilder().setMovimiento("BARROCO").build()).getArtistasList();
        assertThat(barrocos).extracting(Artista::getNombre)
                .contains("Diego Velázquez", "Johannes Vermeer")
                .doesNotContain("Pablo Picasso");
        assertThat(barrocos).allMatch(a -> a.getCantidadObras() >= 1);
    }

    @Test
    void listarCategoriasCuentaObrasYOrdenaPorNombre() {
        CategoriasResponse categorias = stub.listarCategorias(ListarCategoriasRequest.getDefaultInstance());

        List<String> movimientos = categorias.getMovimientosList().stream().map(Categoria::getNombre).toList();
        assertThat(movimientos).contains("Barroco", "Cubismo", "Renacimiento")
                .isSortedAccordingTo(Collator.getInstance(Locale.of("es", "AR")));

        assertThat(cantidad(categorias.getMovimientosList(), "Barroco")).isGreaterThanOrEqualTo(2);
        assertThat(cantidad(categorias.getMovimientosList(), "Renacimiento")).isGreaterThanOrEqualTo(2);
        assertThat(cantidad(categorias.getTematicasList(), "Retrato")).isGreaterThanOrEqualTo(2);
        assertThat(categorias.getTecnicasList()).extracting(Categoria::getNombre).contains("Óleo sobre lienzo");
    }

    private static int cantidad(List<Categoria> categorias, String nombre) {
        return categorias.stream().filter(c -> c.getNombre().equals(nombre)).findFirst().orElseThrow()
                .getCantidadObras();
    }
}
