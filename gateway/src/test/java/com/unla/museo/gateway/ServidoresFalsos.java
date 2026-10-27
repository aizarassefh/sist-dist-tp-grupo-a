package com.unla.museo.gateway;

import com.unla.museo.catalogo.grpc.*;
import com.unla.museo.eventos.grpc.*;
import com.unla.museo.motor.grpc.*;
import com.unla.museo.usuarios.grpc.*;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicios internos falsos, en memoria: devuelven datos fijos y guardan cada
 * pedido recibido para que los tests verifiquen qué se llamó y con qué datos.
 * Se levantan una sola vez por JVM; cada test llama a {@link #reiniciar()}.
 */
final class ServidoresFalsos {

    static final Map<String, List<Object>> PEDIDOS = new ConcurrentHashMap<>();
    /** Orden global de llegada de los pedidos: "Servicio.Metodo". */
    static final List<String> LLAMADAS = Collections.synchronizedList(new ArrayList<>());

    static volatile boolean motorCaido = false;

    static final Obra GIOCONDA = obra(1, "La Gioconda", "Leonardo da Vinci", "Renacimiento", 1503);
    static final Obra NOCHE = obra(2, "La noche estrellada", "Vincent van Gogh", "Postimpresionismo", 1889);
    static final Obra IMPRESION = obra(3, "Impresión, sol naciente", "Claude Monet", "Impresionismo", 0);
    static final List<Obra> TODAS = List.of(GIOCONDA, NOCHE, IMPRESION);

    static {
        try {
            InProcessServerBuilder.forName("fake-backend").directExecutor()
                    .addService(new Catalogo()).addService(new Eventos()).addService(new Usuarios())
                    .build().start();
            InProcessServerBuilder.forName("fake-motor").directExecutor()
                    .addService(new Motor())
                    .build().start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private ServidoresFalsos() {
    }

    /** Fuerza la carga de la clase (y con ella el arranque de los servidores). */
    static void iniciar() {
    }

    static void reiniciar() {
        PEDIDOS.clear();
        LLAMADAS.clear();
        motorCaido = false;
    }

    @SuppressWarnings("unchecked")
    static <T> List<T> pedidos(String servicioYMetodo) {
        return (List<T>) PEDIDOS.getOrDefault(servicioYMetodo, List.of());
    }

    private static void anotar(String clave, Object pedido) {
        PEDIDOS.computeIfAbsent(clave, k -> Collections.synchronizedList(new ArrayList<>())).add(pedido);
        LLAMADAS.add(clave);
    }

    private static <T> void responder(StreamObserver<T> observador, T respuesta) {
        observador.onNext(respuesta);
        observador.onCompleted();
    }

    static Obra obra(long id, String titulo, String artista, String movimiento, int anio) {
        return Obra.newBuilder().setId(id).setTitulo(titulo).setDescripcion("Descripción de " + titulo)
                .setArtista(Artista.newBuilder().setId(id * 10).setNombre(artista)
                        .setNacionalidad("Nacionalidad de " + artista).setMovimiento(movimiento))
                .setAnioCreacion(anio).setTecnica("Óleo").setTematica("Retrato").setMovimiento(movimiento)
                .setDimensiones("77 cm x 53 cm").setUbicacion("Sala " + id).setEnExhibicion(true)
                .setImagenUrl("/obras/" + id + ".jpg").build();
    }

    private static Preferencias preferenciasFijas() {
        return Preferencias.newBuilder().addMovimientos("Impresionismo").addTecnicas("Óleo")
                .addTematicas("Paisaje").build();
    }

    private static Perfil perfil(Preferencias preferencias) {
        return Perfil.newBuilder().setId(7).setEmail("visitante@museo.com").setNombre("Ana")
                .setApellido("Pérez").setRol("VISITANTE").setPreferencias(preferencias).build();
    }

    static class Catalogo extends CatalogoServiceGrpc.CatalogoServiceImplBase {
        @Override
        public void buscarObras(BuscarObrasRequest r, StreamObserver<ObrasResponse> o) {
            anotar("CatalogoService.BuscarObras", r);
            responder(o, ObrasResponse.newBuilder().addAllObras(TODAS).build());
        }

        @Override
        public void obtenerObras(ObtenerObrasRequest r, StreamObserver<ObrasResponse> o) {
            anotar("CatalogoService.ObtenerObras", r);
            ObrasResponse.Builder respuesta = ObrasResponse.newBuilder();
            for (long id : r.getIdsList()) {
                Obra encontrada = TODAS.stream().filter(x -> x.getId() == id).findFirst().orElse(null);
                if (encontrada == null) {
                    o.onError(Status.NOT_FOUND.withDescription("No existe la obra " + id).asRuntimeException());
                    return;
                }
                respuesta.addObras(encontrada);
            }
            responder(o, respuesta.build());
        }

        @Override
        public void listarArtistas(ListarArtistasRequest r, StreamObserver<ArtistasResponse> o) {
            anotar("CatalogoService.ListarArtistas", r);
            responder(o, ArtistasResponse.newBuilder().addArtistas(Artista.newBuilder().setId(4)
                    .setNombre("Claude Monet").setNacionalidad("Francesa").setMovimiento("Impresionismo")
                    .setAnioNacimiento(1840).setAnioFallecimiento(1926).setCantidadObras(2)
                    .setBiografia("Pintor francés")).build());
        }

        @Override
        public void listarCategorias(ListarCategoriasRequest r, StreamObserver<CategoriasResponse> o) {
            anotar("CatalogoService.ListarCategorias", r);
            responder(o, CategoriasResponse.newBuilder()
                    .addMovimientos(Categoria.newBuilder().setNombre("Impresionismo").setCantidadObras(1))
                    .addMovimientos(Categoria.newBuilder().setNombre("Renacimiento").setCantidadObras(1))
                    .addMovimientos(Categoria.newBuilder().setNombre("Postimpresionismo").setCantidadObras(1))
                    .addTecnicas(Categoria.newBuilder().setNombre("Óleo").setCantidadObras(3))
                    .addTematicas(Categoria.newBuilder().setNombre("Retrato").setCantidadObras(3))
                    .build());
        }
    }

    static class Eventos extends EventosServiceGrpc.EventosServiceImplBase {
        private static Evento evento() {
            return Evento.newBuilder().setId(7).setTitulo("Charla: la luz impresionista").setTipo("CHARLA")
                    .setFechaHora("2026-11-05T18:00:00").setDuracionMinutos(60).setCupoMaximo(30)
                    .setInscriptos(12).setMovimiento("Impresionismo").build();
        }

        @Override
        public void listarEventosPorMovimiento(EventosPorMovimientoRequest r, StreamObserver<EventosResponse> o) {
            anotar("EventosService.ListarEventosPorMovimiento", r);
            responder(o, EventosResponse.newBuilder().addEventos(evento()).build());
        }

        @Override
        public void listarEventosDeUsuario(EventosDeUsuarioRequest r, StreamObserver<EventosResponse> o) {
            anotar("EventosService.ListarEventosDeUsuario", r);
            responder(o, EventosResponse.newBuilder().addEventos(evento()).build());
        }
    }

    static class Usuarios extends UsuariosServiceGrpc.UsuariosServiceImplBase {
        @Override
        public void obtenerPerfil(PerfilRequest r, StreamObserver<Perfil> o) {
            anotar("UsuariosService.ObtenerPerfil", r);
            responder(o, perfil(preferenciasFijas()));
        }

        @Override
        public void actualizarPreferencias(ActualizarPreferenciasRequest r, StreamObserver<Perfil> o) {
            anotar("UsuariosService.ActualizarPreferencias", r);
            responder(o, perfil(r.getPreferencias()));
        }

        @Override
        public void registrarInteraccion(RegistrarInteraccionRequest r, StreamObserver<Interaccion> o) {
            anotar("UsuariosService.RegistrarInteraccion", r);
            responder(o, Interaccion.newBuilder().setObraId(r.getObraId()).setTipo(r.getTipo())
                    .setFecha("2026-10-27T18:30:00").build());
        }

        @Override
        public void listarInteracciones(PerfilRequest r, StreamObserver<InteraccionesResponse> o) {
            anotar("UsuariosService.ListarInteracciones", r);
            responder(o, InteraccionesResponse.newBuilder().addInteracciones(
                    Interaccion.newBuilder().setObraId(2).setTipo("INTERES").setFecha("2026-10-20T10:00:00")).build());
        }
    }

    static class Motor extends MotorServiceGrpc.MotorServiceImplBase {
        private boolean caido(StreamObserver<?> o) {
            if (motorCaido) {
                o.onError(Status.UNAVAILABLE.withDescription("motor caído").asRuntimeException());
            }
            return motorCaido;
        }

        @Override
        public void armarRecorrido(ArmarRecorridoRequest r, StreamObserver<Recorrido> o) {
            anotar("MotorService.ArmarRecorrido", r);
            if (caido(o)) {
                return;
            }
            responder(o, Recorrido.newBuilder()
                    .addParadas(Parada.newBuilder().setOrden(1).setObraId(3).setSala("Sala 3")
                            .setMinutosSugeridos(6).setMotivo("Coincide con tu interés en Impresionismo"))
                    .addParadas(Parada.newBuilder().setOrden(2).setObraId(1).setSala("Sala 1")
                            .setMinutosSugeridos(5).setMotivo("Obra destacada"))
                    .setDuracionTotalMinutos(52).addNotas("Quedaron 1 obras afuera").build());
        }

        @Override
        public void recomendar(RecomendarRequest r, StreamObserver<RecomendacionesResponse> o) {
            anotar("MotorService.Recomendar", r);
            if (caido(o)) {
                return;
            }
            responder(o, RecomendacionesResponse.newBuilder().setCriterio("PERSONALIZADO")
                    .addRecomendaciones(Recomendacion.newBuilder().setObraId(3).setPuntaje(7.5)
                            .addMotivos("Es del Impresionismo, uno de tus movimientos preferidos")).build());
        }

        @Override
        public void compararObras(CompararObrasRequest r, StreamObserver<Comparacion> o) {
            anotar("MotorService.CompararObras", r);
            if (caido(o)) {
                return;
            }
            responder(o, Comparacion.newBuilder()
                    .addAtributos(AtributoComparado.newBuilder().setNombre("Movimiento")
                            .addValores("Renacimiento").addValores("Postimpresionismo").setCoinciden(false))
                    .addObservaciones("Entre la más antigua y la más reciente hay 386 años").build());
        }
    }
}
