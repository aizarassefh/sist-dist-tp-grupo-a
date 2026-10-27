package com.unla.museo.catalogo;

import com.unla.museo.catalogo.grpc.BuscarObrasRequest;
import com.unla.museo.catalogo.grpc.CatalogoServiceGrpc;
import com.unla.museo.catalogo.grpc.Categoria;
import com.unla.museo.catalogo.grpc.ListarCategoriasRequest;
import com.unla.museo.catalogo.grpc.service.CatalogoGrpcService;
import com.unla.museo.comun.ServidorGrpcEnProceso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica las 18 obras de ejemplo completas. Solo corre si se lo pide con
 * -Dapp.datos-de-ejemplo-ampliados=true (en el argLine de surefire): el perfil
 * test deja las 9 obras ampliadas afuera porque los tests de la Entrega 1
 * asumen exactamente las 9 originales.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "app.datos-de-ejemplo-ampliados", matches = "true")
class CatalogoDatosAmpliadosTest {

    @Autowired
    private CatalogoGrpcService servicio;

    @Test
    void cadaMovimientoTieneAlMenosDosObras() throws Exception {
        try (ServidorGrpcEnProceso servidor = new ServidorGrpcEnProceso(servicio)) {
            var stub = CatalogoServiceGrpc.newBlockingStub(servidor.canal());

            assertThat(stub.buscarObras(BuscarObrasRequest.getDefaultInstance()).getObrasCount()).isEqualTo(18);
            var movimientos = stub.listarCategorias(ListarCategoriasRequest.getDefaultInstance())
                    .getMovimientosList();
            for (String movimiento : new String[] {"Renacimiento", "Barroco", "Impresionismo", "Postimpresionismo",
                    "Expresionismo", "Surrealismo", "Cubismo"}) {
                assertThat(movimientos.stream().filter(c -> c.getNombre().equals(movimiento))
                        .mapToInt(Categoria::getCantidadObras).sum())
                        .as("obras de " + movimiento).isGreaterThanOrEqualTo(2);
            }
            assertThat(stub.buscarObras(BuscarObrasRequest.newBuilder().setSoloEnExhibicion(true).build())
                    .getObrasCount()).isLessThan(18);
        }
    }
}
