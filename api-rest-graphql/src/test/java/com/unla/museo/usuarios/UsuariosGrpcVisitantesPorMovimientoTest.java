package com.unla.museo.usuarios;

import com.unla.museo.comun.ServidorGrpcEnProceso;
import com.unla.museo.usuarios.grpc.ActualizarPreferenciasRequest;
import com.unla.museo.usuarios.grpc.Preferencias;
import com.unla.museo.usuarios.grpc.UsuariosServiceGrpc;
import com.unla.museo.usuarios.grpc.Visitante;
import com.unla.museo.usuarios.grpc.VisitantesPorMovimientoRequest;
import com.unla.museo.usuarios.grpc.service.UsuariosGrpcService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ListarVisitantesPorMovimiento (Entrega 3). Usa un movimiento propio de este
 * test para no depender de las preferencias que escriben los otros tests.
 */
@SpringBootTest
@ActiveProfiles("test")
class UsuariosGrpcVisitantesPorMovimientoTest {

    @Autowired
    private UsuariosGrpcService servicio;

    private ServidorGrpcEnProceso servidor;
    private UsuariosServiceGrpc.UsuariosServiceBlockingStub stub;

    @BeforeEach
    void levantarServidor() throws Exception {
        servidor = new ServidorGrpcEnProceso(servicio);
        stub = UsuariosServiceGrpc.newBlockingStub(servidor.canal());
    }

    @AfterEach
    void bajarServidor() throws Exception {
        servidor.close();
    }

    private void fijarMovimiento(String email, String movimiento) {
        stub.actualizarPreferencias(ActualizarPreferenciasRequest.newBuilder().setEmail(email)
                .setPreferencias(Preferencias.newBuilder().addMovimientos(movimiento)).build());
    }

    private List<Visitante> buscar(String movimiento) {
        return stub.listarVisitantesPorMovimiento(
                VisitantesPorMovimientoRequest.newBuilder().setMovimiento(movimiento).build()).getVisitantesList();
    }

    @Test
    void devuelveSoloVisitantesIgnorandoMayusculasYAcentos() {
        fijarMovimiento("martin.rodriguez@test.com", "Vanguardia Ácida");
        // Un curador con el mismo gusto no es destinatario: la consulta es de visitantes.
        fijarMovimiento("curador@test.com", "Vanguardia Ácida");

        List<Visitante> encontrados = buscar("  vanguardia ACIDA ");

        assertThat(encontrados).extracting(Visitante::getEmail).containsExactly("martin.rodriguez@test.com");
        assertThat(encontrados.get(0).getNombre()).isEqualTo("Martín");
        assertThat(encontrados.get(0).getApellido()).isEqualTo("Rodríguez");
        assertThat(encontrados.get(0).getId()).isPositive();
    }

    @Test
    void elVisitanteDeEjemploTieneImpresionismo() {
        assertThat(buscar("impresionismo")).extracting(Visitante::getEmail).contains("visitante@test.com");
    }

    @Test
    void movimientoSinInteresadosOVacioDevuelveListaVacia() {
        assertThat(buscar("Movimiento que nadie eligió")).isEmpty();
        assertThat(buscar("")).isEmpty();
        assertThat(buscar("   ")).isEmpty();
    }
}
