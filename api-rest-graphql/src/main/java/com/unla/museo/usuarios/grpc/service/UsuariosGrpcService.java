package com.unla.museo.usuarios.grpc.service;

import com.unla.museo.comun.GrpcRespuestas;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import com.unla.museo.usuarios.entity.InteraccionUsuario;
import com.unla.museo.usuarios.entity.InteraccionUsuario.TipoInteraccion;
import com.unla.museo.usuarios.entity.PreferenciasUsuario;
import com.unla.museo.usuarios.grpc.ActualizarPreferenciasRequest;
import com.unla.museo.usuarios.grpc.Interaccion;
import com.unla.museo.usuarios.grpc.InteraccionesResponse;
import com.unla.museo.usuarios.grpc.Perfil;
import com.unla.museo.usuarios.grpc.PerfilRequest;
import com.unla.museo.usuarios.grpc.Preferencias;
import com.unla.museo.usuarios.grpc.RegistrarInteraccionRequest;
import com.unla.museo.usuarios.grpc.UsuariosServiceGrpc;
import com.unla.museo.usuarios.grpc.mapper.UsuariosGrpcMapper;
import com.unla.museo.usuarios.repository.InteraccionUsuarioRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

// Transacción de solo lectura a nivel de clase: gRPC corre fuera de un request
// web, y sin transacción leer una relación LAZY tira LazyInitializationException.
@GrpcService
@Transactional(readOnly = true)
public class UsuariosGrpcService extends UsuariosServiceGrpc.UsuariosServiceImplBase {

    private final UsuarioRepository usuarioRepository;
    private final InteraccionUsuarioRepository interaccionRepository;
    private final Clock clock;

    public UsuariosGrpcService(@Qualifier("UsuarioSQLRepositoryImpl") UsuarioRepository usuarioRepository,
                               InteraccionUsuarioRepository interaccionRepository, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.interaccionRepository = interaccionRepository;
        this.clock = clock;
    }

    @Override
    public void obtenerPerfil(PerfilRequest pedido, StreamObserver<Perfil> respuesta) {
        GrpcRespuestas.responder(respuesta, () -> UsuariosGrpcMapper.aPerfil(buscarUsuario(pedido.getEmail())));
    }

    @Override
    @Transactional
    public void actualizarPreferencias(ActualizarPreferenciasRequest pedido, StreamObserver<Perfil> respuesta) {
        GrpcRespuestas.responder(respuesta, () -> actualizarPreferencias(pedido));
    }

    private Perfil actualizarPreferencias(ActualizarPreferenciasRequest pedido) {
        UsuarioEntity usuario = buscarUsuario(pedido.getEmail());
        Preferencias nuevas = pedido.getPreferencias();

        // Se vacían y rellenan las listas existentes en lugar de reemplazarlas:
        // Hibernate rastrea la colección original y así genera el delete/insert justo.
        PreferenciasUsuario preferencias = usuario.getPreferencias();
        reemplazar(preferencias.getMovimientos(), nuevas.getMovimientosList());
        reemplazar(preferencias.getTecnicas(), nuevas.getTecnicasList());
        reemplazar(preferencias.getTematicas(), nuevas.getTematicasList());
        usuario.setPreferencias(preferencias);

        return UsuariosGrpcMapper.aPerfil(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void registrarInteraccion(RegistrarInteraccionRequest pedido, StreamObserver<Interaccion> respuesta) {
        GrpcRespuestas.responder(respuesta, () -> registrarInteraccion(pedido));
    }

    private Interaccion registrarInteraccion(RegistrarInteraccionRequest pedido) {
        TipoInteraccion tipo = parsearTipo(pedido.getTipo());
        if (pedido.getObraId() <= 0) {
            throw Status.INVALID_ARGUMENT.withDescription("El id de obra debe ser mayor que cero").asRuntimeException();
        }
        UsuarioEntity usuario = buscarUsuario(pedido.getEmail());

        InteraccionUsuario interaccion = new InteraccionUsuario();
        interaccion.setUsuario(usuario);
        interaccion.setObraId(pedido.getObraId());
        interaccion.setTipo(tipo);
        interaccion.setFecha(LocalDateTime.now(clock));

        return UsuariosGrpcMapper.aInteraccion(interaccionRepository.save(interaccion));
    }

    @Override
    public void listarInteracciones(PerfilRequest pedido, StreamObserver<InteraccionesResponse> respuesta) {
        GrpcRespuestas.responder(respuesta, () -> listarInteracciones(pedido));
    }

    private InteraccionesResponse listarInteracciones(PerfilRequest pedido) {
        UsuarioEntity usuario = buscarUsuario(pedido.getEmail());
        InteraccionesResponse.Builder resultado = InteraccionesResponse.newBuilder();
        for (InteraccionUsuario i : interaccionRepository.findByUsuarioIdOrderByFechaDescIdDesc(usuario.getId())) {
            resultado.addInteracciones(UsuariosGrpcMapper.aInteraccion(i));
        }
        return resultado.build();
    }

    private UsuarioEntity buscarUsuario(String email) {
        String limpio = email.trim();
        if (limpio.isEmpty()) {
            throw Status.INVALID_ARGUMENT.withDescription("Hay que indicar el email del usuario").asRuntimeException();
        }
        return usuarioRepository.findByEmail(limpio)
                .orElseThrow(() -> Status.NOT_FOUND.withDescription("No existe el usuario con email " + limpio)
                        .asRuntimeException());
    }

    private static TipoInteraccion parsearTipo(String tipo) {
        try {
            return TipoInteraccion.valueOf(tipo.trim());
        } catch (IllegalArgumentException e) {
            throw Status.INVALID_ARGUMENT
                    .withDescription("Tipo de interacción desconocido: '" + tipo + "' (se espera VISTA o INTERES)")
                    .asRuntimeException();
        }
    }

    // Sin repetidos ni vacíos, conservando el orden en que llegaron.
    private static void reemplazar(List<String> destino, List<String> valores) {
        LinkedHashSet<String> limpios = new LinkedHashSet<>();
        for (String valor : valores) {
            if (!valor.isBlank()) {
                limpios.add(valor.trim());
            }
        }
        destino.clear();
        destino.addAll(limpios);
    }
}
