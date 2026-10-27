package com.unla.museo.usuarios;

import com.unla.museo.comun.ServidorGrpcEnProceso;
import com.unla.museo.usuarios.grpc.ActualizarPreferenciasRequest;
import com.unla.museo.usuarios.grpc.Interaccion;
import com.unla.museo.usuarios.grpc.Perfil;
import com.unla.museo.usuarios.grpc.PerfilRequest;
import com.unla.museo.usuarios.grpc.Preferencias;
import com.unla.museo.usuarios.grpc.RegistrarInteraccionRequest;
import com.unla.museo.usuarios.grpc.UsuariosServiceGrpc;
import com.unla.museo.usuarios.grpc.service.UsuariosGrpcService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Llama a UsuariosService por un servidor gRPC en memoria. Los tests que
 * escriben usan usuarios distintos entre sí (y distintos de visitante@test.com,
 * cuyas preferencias de ejemplo se leen) para no pisarse.
 */
@SpringBootTest
@ActiveProfiles("test")
class UsuariosGrpcServiceTest {

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

    private static PerfilRequest perfil(String email) {
        return PerfilRequest.newBuilder().setEmail(email).build();
    }

    private static void assertEstado(Runnable llamada, Status.Code codigo) {
        assertThatThrownBy(llamada::run)
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        e -> assertThat(e.getStatus().getCode()).isEqualTo(codigo));
    }

    @Test
    void obtenerPerfilDevuelveDatosYPreferenciasDeEjemplo() {
        Perfil perfil = stub.obtenerPerfil(perfil("visitante@test.com"));

        assertThat(perfil.getEmail()).isEqualTo("visitante@test.com");
        assertThat(perfil.getNombre()).isEqualTo("Carlos");
        assertThat(perfil.getRol()).isEqualTo("VISITANTE");
        assertThat(perfil.getPreferencias().getMovimientosList()).containsExactly("Impresionismo");
        assertThat(perfil.getPreferencias().getTecnicasList()).containsExactly("Óleo");
        assertThat(perfil.getPreferencias().getTematicasList()).containsExactly("Paisaje");
    }

    @Test
    void obtenerPerfilDeUsuarioSinPreferenciasLasDevuelveVacias() {
        Perfil perfil = stub.obtenerPerfil(perfil("admin@test.com"));

        assertThat(perfil.getRol()).isEqualTo("ADMINISTRADOR");
        assertThat(perfil.getPreferencias().getMovimientosList()).isEmpty();
    }

    @Test
    void actualizarPreferenciasReemplazaLasAnteriores() {
        Preferencias primeras = Preferencias.newBuilder().addMovimientos("Barroco").addMovimientos("Cubismo")
                .addTecnicas("Temple").addTematicas("Retrato").build();
        stub.actualizarPreferencias(ActualizarPreferenciasRequest.newBuilder()
                .setEmail("sofia.lopez@test.com").setPreferencias(primeras).build());

        // La segunda llamada no suma: reemplaza (y descarta vacíos y repetidos).
        Preferencias segundas = Preferencias.newBuilder().addMovimientos("Surrealismo").addMovimientos("Surrealismo")
                .addMovimientos(" ").build();
        Perfil devuelto = stub.actualizarPreferencias(ActualizarPreferenciasRequest.newBuilder()
                .setEmail("sofia.lopez@test.com").setPreferencias(segundas).build());

        assertThat(devuelto.getPreferencias().getMovimientosList()).containsExactly("Surrealismo");
        assertThat(devuelto.getPreferencias().getTecnicasList()).isEmpty();

        // Persistió: una lectura nueva ve lo mismo.
        Perfil releido = stub.obtenerPerfil(perfil("sofia.lopez@test.com"));
        assertThat(releido.getPreferencias().getMovimientosList()).containsExactly("Surrealismo");
        assertThat(releido.getPreferencias().getTematicasList()).isEmpty();
    }

    @Test
    void registrarYListarInteraccionesDevuelveLaMasRecientePrimero() {
        Interaccion vista = stub.registrarInteraccion(RegistrarInteraccionRequest.newBuilder()
                .setEmail("nicolas.diaz@test.com").setObraId(101L).setTipo("VISTA").build());
        stub.registrarInteraccion(RegistrarInteraccionRequest.newBuilder()
                .setEmail("nicolas.diaz@test.com").setObraId(202L).setTipo("INTERES").build());

        assertThat(vista.getObraId()).isEqualTo(101L);
        assertThat(vista.getTipo()).isEqualTo("VISTA");
        assertThat(vista.getFecha()).isNotBlank();

        List<Interaccion> interacciones = stub.listarInteracciones(perfil("nicolas.diaz@test.com"))
                .getInteraccionesList();
        assertThat(interacciones).extracting(Interaccion::getObraId).containsExactly(202L, 101L);
        assertThat(interacciones).extracting(Interaccion::getTipo).containsExactly("INTERES", "VISTA");
    }

    @Test
    void elVisitanteDeEjemploTieneUnaVistaYUnInteres() {
        List<Interaccion> interacciones = stub.listarInteracciones(perfil("visitante@test.com"))
                .getInteraccionesList();

        assertThat(interacciones).extracting(Interaccion::getTipo).contains("VISTA", "INTERES");
    }

    @Test
    void registrarInteraccionConTipoDesconocidoEsInvalid() {
        assertEstado(() -> stub.registrarInteraccion(RegistrarInteraccionRequest.newBuilder()
                .setEmail("camila.romero@test.com").setObraId(1L).setTipo("FAVORITO").build()),
                Status.Code.INVALID_ARGUMENT);
        assertEstado(() -> stub.registrarInteraccion(RegistrarInteraccionRequest.newBuilder()
                .setEmail("camila.romero@test.com").setObraId(0L).setTipo("VISTA").build()),
                Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void emailInexistenteDevuelveNotFoundEnTodasLasOperaciones() {
        assertThatThrownBy(() -> stub.obtenerPerfil(perfil("nadie@test.com")))
                .isInstanceOfSatisfying(StatusRuntimeException.class, e -> {
                    assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
                    assertThat(e.getStatus().getDescription()).contains("nadie@test.com");
                });
        assertEstado(() -> stub.listarInteracciones(perfil("nadie@test.com")), Status.Code.NOT_FOUND);
        assertEstado(() -> stub.actualizarPreferencias(ActualizarPreferenciasRequest.newBuilder()
                .setEmail("nadie@test.com").build()), Status.Code.NOT_FOUND);
        assertEstado(() -> stub.registrarInteraccion(RegistrarInteraccionRequest.newBuilder()
                .setEmail("nadie@test.com").setObraId(1L).setTipo("VISTA").build()), Status.Code.NOT_FOUND);
    }

    @Test
    void emailVacioEsInvalid() {
        assertEstado(() -> stub.obtenerPerfil(PerfilRequest.getDefaultInstance()), Status.Code.INVALID_ARGUMENT);
    }
}
