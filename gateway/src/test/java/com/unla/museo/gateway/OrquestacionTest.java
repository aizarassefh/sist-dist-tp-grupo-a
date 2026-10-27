package com.unla.museo.gateway;

import com.unla.museo.catalogo.grpc.BuscarObrasRequest;
import com.unla.museo.catalogo.grpc.ListarArtistasRequest;
import com.unla.museo.catalogo.grpc.ObtenerObrasRequest;
import com.unla.museo.eventos.grpc.EventosPorMovimientoRequest;
import com.unla.museo.motor.grpc.ArmarRecorridoRequest;
import com.unla.museo.motor.grpc.CompararObrasRequest;
import com.unla.museo.motor.grpc.RecomendarRequest;
import com.unla.museo.usuarios.grpc.ActualizarPreferenciasRequest;
import com.unla.museo.usuarios.grpc.PerfilRequest;
import com.unla.museo.usuarios.grpc.RegistrarInteraccionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrquestacionTest extends BaseGatewayTest {

    private WebTestClient.RequestBodySpec postVisitante(String uri) {
        return cliente.post().uri(uri).header(HttpHeaders.AUTHORIZATION, visitante())
                .contentType(MediaType.APPLICATION_JSON);
    }

    private WebTestClient.RequestHeadersSpec<?> getVisitante(String uri) {
        return cliente.get().uri(uri).header(HttpHeaders.AUTHORIZATION, visitante());
    }

    // ---------- Recorridos ----------

    @Test
    void recorridoConPreferenciasLlamaUsuariosCatalogoYMotorEnOrden() {
        postVisitante("/api/v2/recorridos").bodyValue("""
                        {"duracionMinutos":60,"movimientos":["Impresionismo"],"tecnicas":["Óleo"],
                         "tematicas":["Paisaje"],"cantidadObras":8,"usarPreferencias":true}""")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paradas.length()").isEqualTo(2)
                .jsonPath("$.paradas[0].orden").isEqualTo(1)
                .jsonPath("$.paradas[0].sala").isEqualTo("Sala 3")
                .jsonPath("$.paradas[0].minutosSugeridos").isEqualTo(6)
                .jsonPath("$.paradas[0].motivo").isEqualTo("Coincide con tu interés en Impresionismo")
                .jsonPath("$.paradas[0].obra.id").isEqualTo(3)
                .jsonPath("$.paradas[0].obra.titulo").isEqualTo("Impresión, sol naciente")
                .jsonPath("$.paradas[0].obra.anioCreacion").isEmpty()
                .jsonPath("$.paradas[0].obra.artista.nombre").isEqualTo("Claude Monet")
                .jsonPath("$.paradas[0].obra.enExhibicion").isEqualTo(true)
                .jsonPath("$.paradas[1].obra.anioCreacion").isEqualTo(1503)
                .jsonPath("$.duracionTotalMinutos").isEqualTo(52)
                .jsonPath("$.notas[0]").isEqualTo("Quedaron 1 obras afuera")
                .jsonPath("$.trazas.length()").isEqualTo(3)
                .jsonPath("$.trazas[0].servicio").isEqualTo("UsuariosService")
                .jsonPath("$.trazas[0].metodo").isEqualTo("ObtenerPerfil")
                .jsonPath("$.trazas[0].ms").isNumber()
                .jsonPath("$.trazas[1].servicio").isEqualTo("CatalogoService")
                .jsonPath("$.trazas[1].metodo").isEqualTo("BuscarObras")
                .jsonPath("$.trazas[2].servicio").isEqualTo("MotorService")
                .jsonPath("$.trazas[2].metodo").isEqualTo("ArmarRecorrido");

        assertEquals(List.of("UsuariosService.ObtenerPerfil", "CatalogoService.BuscarObras",
                "MotorService.ArmarRecorrido"), ServidoresFalsos.LLAMADAS);

        PerfilRequest perfil = ServidoresFalsos.<PerfilRequest>pedidos("UsuariosService.ObtenerPerfil").get(0);
        assertEquals(EMAIL, perfil.getEmail(), "el email sale del token");

        BuscarObrasRequest busqueda = ServidoresFalsos.<BuscarObrasRequest>pedidos("CatalogoService.BuscarObras").get(0);
        assertEquals(List.of("Impresionismo"), busqueda.getMovimientosList());
        assertEquals(List.of("Óleo"), busqueda.getTecnicasList());
        assertEquals(List.of("Paisaje"), busqueda.getTematicasList());
        assertTrue(busqueda.getSoloEnExhibicion());

        ArmarRecorridoRequest motor = ServidoresFalsos.<ArmarRecorridoRequest>pedidos("MotorService.ArmarRecorrido").get(0);
        assertEquals(3, motor.getCandidatasCount(), "pasa las candidatas del catálogo tal cual");
        assertEquals(60, motor.getDuracionMinutos());
        assertEquals(8, motor.getCantidadMaxima());
        assertTrue(motor.hasPreferencias());
        assertEquals(List.of("Impresionismo"), motor.getPreferencias().getMovimientosList());
    }

    @Test
    void recorridoSinUsarPreferenciasNoLlamaAUsuariosNiPasaPreferenciasAlMotor() {
        postVisitante("/api/v2/recorridos").bodyValue("{}")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.trazas.length()").isEqualTo(2);

        assertEquals(List.of("CatalogoService.BuscarObras", "MotorService.ArmarRecorrido"), ServidoresFalsos.LLAMADAS);
        ArmarRecorridoRequest motor = ServidoresFalsos.<ArmarRecorridoRequest>pedidos("MotorService.ArmarRecorrido").get(0);
        assertFalse(motor.hasPreferencias());
        assertEquals(0, motor.getDuracionMinutos());
        BuscarObrasRequest busqueda = ServidoresFalsos.<BuscarObrasRequest>pedidos("CatalogoService.BuscarObras").get(0);
        assertTrue(busqueda.getSoloEnExhibicion());
        assertEquals(0, busqueda.getMovimientosCount());
    }

    @Test
    void recorridoAceptaPedidoSinCuerpo() {
        cliente.post().uri("/api/v2/recorridos").header(HttpHeaders.AUTHORIZATION, visitante())
                .exchange().expectStatus().isOk();
    }

    @Test
    void recorridoConValoresFueraDeRangoDa400() {
        for (String cuerpo : List.of("{\"duracionMinutos\":14}", "{\"duracionMinutos\":481}",
                "{\"cantidadObras\":0}", "{\"cantidadObras\":31}")) {
            postVisitante("/api/v2/recorridos").bodyValue(cuerpo)
                    .exchange().expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.estado").isEqualTo("BAD_REQUEST");
        }
        postVisitante("/api/v2/recorridos").bodyValue("{no es json")
                .exchange().expectStatus().isBadRequest();
        assertTrue(ServidoresFalsos.LLAMADAS.isEmpty());
    }

    // ---------- Comparaciones ----------

    @Test
    void comparacionLlamaCatalogoYLuegoMotorConLasObrasEnOrden() {
        postVisitante("/api/v2/comparaciones").bodyValue("{\"obraIds\":[2,1]}")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.obras[0].id").isEqualTo(2)
                .jsonPath("$.obras[1].titulo").isEqualTo("La Gioconda")
                .jsonPath("$.obras[1].artista.nacionalidad").isEqualTo("Nacionalidad de Leonardo da Vinci")
                .jsonPath("$.atributos[0].nombre").isEqualTo("Movimiento")
                .jsonPath("$.atributos[0].valores[1]").isEqualTo("Postimpresionismo")
                .jsonPath("$.atributos[0].coinciden").isEqualTo(false)
                .jsonPath("$.observaciones[0]").isEqualTo("Entre la más antigua y la más reciente hay 386 años")
                .jsonPath("$.trazas.length()").isEqualTo(2)
                .jsonPath("$.trazas[0].servicio").isEqualTo("CatalogoService")
                .jsonPath("$.trazas[0].metodo").isEqualTo("ObtenerObras")
                .jsonPath("$.trazas[1].servicio").isEqualTo("MotorService")
                .jsonPath("$.trazas[1].metodo").isEqualTo("CompararObras");

        ObtenerObrasRequest catalogo = ServidoresFalsos.<ObtenerObrasRequest>pedidos("CatalogoService.ObtenerObras").get(0);
        assertEquals(List.of(2L, 1L), catalogo.getIdsList());
        CompararObrasRequest motor = ServidoresFalsos.<CompararObrasRequest>pedidos("MotorService.CompararObras").get(0);
        assertEquals(List.of(2L, 1L), motor.getObrasList().stream().map(o -> o.getId()).toList());
    }

    @Test
    void comparacionConIdsInvalidosDa400SinLlamarAServicios() {
        for (String cuerpo : List.of("{\"obraIds\":[1]}", "{\"obraIds\":[1,1]}", "{\"obraIds\":[1,2,3,4,5,6,7]}",
                "{\"obraIds\":[]}", "{}")) {
            postVisitante("/api/v2/comparaciones").bodyValue(cuerpo)
                    .exchange().expectStatus().isBadRequest();
        }
        assertTrue(ServidoresFalsos.LLAMADAS.isEmpty());
    }

    @Test
    void comparacionConObraInexistenteDa404() {
        postVisitante("/api/v2/comparaciones").bodyValue("{\"obraIds\":[1,99]}")
                .exchange().expectStatus().isNotFound()
                .expectBody().jsonPath("$.mensaje").isEqualTo("No existe la obra 99");
        assertNull(ServidoresFalsos.PEDIDOS.get("MotorService.CompararObras"));
    }

    // ---------- Recomendaciones ----------

    @Test
    void recomendacionesJuntaDatosEnDosEtapasYLlamaAlMotor() {
        getVisitante("/api/v2/recomendaciones")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.recomendaciones.length()").isEqualTo(1)
                .jsonPath("$.recomendaciones[0].obra.titulo").isEqualTo("Impresión, sol naciente")
                .jsonPath("$.recomendaciones[0].puntaje").isEqualTo(7.5)
                .jsonPath("$.recomendaciones[0].motivos[0]").isEqualTo("Es del Impresionismo, uno de tus movimientos preferidos")
                .jsonPath("$.criterio").isEqualTo("PERSONALIZADO")
                .jsonPath("$.preferencias.movimientos[0]").isEqualTo("Impresionismo")
                .jsonPath("$.preferencias.tecnicas[0]").isEqualTo("Óleo")
                .jsonPath("$.trazas.length()").isEqualTo(6);

        List<String> llamadas = ServidoresFalsos.LLAMADAS;
        assertEquals(6, llamadas.size());
        // Las dos primeras etapas son paralelas: dentro de cada una el orden es libre.
        assertEquals(java.util.Set.of("UsuariosService.ObtenerPerfil", "UsuariosService.ListarInteracciones",
                "EventosService.ListarEventosDeUsuario"), new java.util.HashSet<>(llamadas.subList(0, 3)));
        assertEquals(java.util.Set.of("CatalogoService.BuscarObras", "CatalogoService.ObtenerObras"),
                new java.util.HashSet<>(llamadas.subList(3, 5)));
        assertEquals("MotorService.Recomendar", llamadas.get(5));

        ObtenerObrasRequest conInteraccion = ServidoresFalsos.<ObtenerObrasRequest>pedidos("CatalogoService.ObtenerObras").get(0);
        assertEquals(List.of(2L), conInteraccion.getIdsList());
        BuscarObrasRequest busqueda = ServidoresFalsos.<BuscarObrasRequest>pedidos("CatalogoService.BuscarObras").get(0);
        assertEquals(0, busqueda.getMovimientosCount());
        assertFalse(busqueda.getSoloEnExhibicion());

        RecomendarRequest motor = ServidoresFalsos.<RecomendarRequest>pedidos("MotorService.Recomendar").get(0);
        assertEquals(3, motor.getCandidatasCount());
        assertEquals(1, motor.getInteraccionesCount());
        assertEquals(1, motor.getObrasConInteraccionCount());
        assertEquals(2L, motor.getObrasConInteraccion(0).getId());
        assertEquals(1, motor.getEventosInscriptosCount());
        assertEquals(List.of("Impresionismo"), motor.getPreferencias().getMovimientosList());
        assertEquals(6, motor.getCantidad(), "cantidad por defecto");
    }

    @Test
    void recomendacionesRespetaLaCantidadYLaValida() {
        getVisitante("/api/v2/recomendaciones?cantidad=3").exchange().expectStatus().isOk();
        assertEquals(3, ServidoresFalsos.<RecomendarRequest>pedidos("MotorService.Recomendar").get(0).getCantidad());

        for (String cantidad : List.of("0", "21", "abc")) {
            getVisitante("/api/v2/recomendaciones?cantidad=" + cantidad)
                    .exchange().expectStatus().isBadRequest();
        }
    }

    // ---------- Preferencias e interacciones ----------

    @Test
    void obtenerPreferenciasUsaElEmailDelToken() {
        getVisitante("/api/v2/preferencias").exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.movimientos[0]").isEqualTo("Impresionismo")
                .jsonPath("$.tecnicas[0]").isEqualTo("Óleo")
                .jsonPath("$.tematicas[0]").isEqualTo("Paisaje");
        assertEquals(EMAIL, ServidoresFalsos.<PerfilRequest>pedidos("UsuariosService.ObtenerPerfil").get(0).getEmail());
    }

    @Test
    void actualizarPreferenciasReemplazaLasDelToken() {
        cliente.put().uri("/api/v2/preferencias").header(HttpHeaders.AUTHORIZATION, visitante())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"otro@museo.com\",\"movimientos\":[\"Cubismo\"],\"tecnicas\":[\"Acuarela\"]}")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.movimientos[0]").isEqualTo("Cubismo")
                .jsonPath("$.tecnicas[0]").isEqualTo("Acuarela")
                .jsonPath("$.tematicas.length()").isEqualTo(0);

        ActualizarPreferenciasRequest pedido = ServidoresFalsos
                .<ActualizarPreferenciasRequest>pedidos("UsuariosService.ActualizarPreferencias").get(0);
        assertEquals(EMAIL, pedido.getEmail(), "el email del cuerpo se ignora");
        assertEquals(List.of("Cubismo"), pedido.getPreferencias().getMovimientosList());
    }

    @Test
    void interaccionVerificaLaObraYLaRegistra() {
        postVisitante("/api/v2/interacciones").bodyValue("{\"obraId\":3,\"tipo\":\"INTERES\"}")
                .exchange().expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.obraId").isEqualTo(3)
                .jsonPath("$.tipo").isEqualTo("INTERES")
                .jsonPath("$.fecha").isEqualTo("2026-10-27T18:30:00");

        assertEquals(List.of("CatalogoService.ObtenerObras", "UsuariosService.RegistrarInteraccion"),
                ServidoresFalsos.LLAMADAS);
        RegistrarInteraccionRequest pedido = ServidoresFalsos
                .<RegistrarInteraccionRequest>pedidos("UsuariosService.RegistrarInteraccion").get(0);
        assertEquals(EMAIL, pedido.getEmail());
        assertEquals(3L, pedido.getObraId());
        assertEquals("INTERES", pedido.getTipo());
    }

    @Test
    void interaccionConObraInexistenteDa404SinLlamarAUsuarios() {
        postVisitante("/api/v2/interacciones").bodyValue("{\"obraId\":99,\"tipo\":\"VISTA\"}")
                .exchange().expectStatus().isNotFound()
                .expectBody().jsonPath("$.codigo").isEqualTo(404).jsonPath("$.estado").isEqualTo("NOT_FOUND");

        assertEquals(List.of("CatalogoService.ObtenerObras"), ServidoresFalsos.LLAMADAS);
        assertNull(ServidoresFalsos.PEDIDOS.get("UsuariosService.RegistrarInteraccion"));
    }

    @Test
    void interaccionConTipoOFormaInvalidaDa400() {
        for (String cuerpo : List.of("{\"obraId\":3,\"tipo\":\"ME_GUSTA\"}", "{\"obraId\":3}",
                "{\"tipo\":\"VISTA\"}")) {
            postVisitante("/api/v2/interacciones").bodyValue(cuerpo).exchange().expectStatus().isBadRequest();
        }
        assertTrue(ServidoresFalsos.LLAMADAS.isEmpty());
    }

    // ---------- Exploraciones y categorías ----------

    @Test
    void exploracionLlamaCuatroServiciosSinUsarElMotor() {
        getVisitante("/api/v2/exploraciones/Impresionismo")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.movimiento").isEqualTo("Impresionismo")
                .jsonPath("$.obras.length()").isEqualTo(3)
                .jsonPath("$.artistas[0].nombre").isEqualTo("Claude Monet")
                .jsonPath("$.artistas[0].anioNacimiento").isEqualTo(1840)
                .jsonPath("$.artistas[0].cantidadObras").isEqualTo(2)
                .jsonPath("$.artistas[0].biografia").isEqualTo("Pintor francés")
                .jsonPath("$.eventos[0].titulo").isEqualTo("Charla: la luz impresionista")
                .jsonPath("$.eventos[0].fechaHora").isEqualTo("2026-11-05T18:00:00")
                .jsonPath("$.eventos[0].inscriptos").isEqualTo(12)
                .jsonPath("$.otrosMovimientos.length()").isEqualTo(2)
                .jsonPath("$.otrosMovimientos[0].nombre").isEqualTo("Renacimiento")
                .jsonPath("$.trazas.length()").isEqualTo(4);

        assertEquals(java.util.Set.of("CatalogoService.BuscarObras", "CatalogoService.ListarArtistas",
                "EventosService.ListarEventosPorMovimiento", "CatalogoService.ListarCategorias"),
                new java.util.HashSet<>(ServidoresFalsos.LLAMADAS));
        assertEquals(List.of("Impresionismo"),
                ServidoresFalsos.<BuscarObrasRequest>pedidos("CatalogoService.BuscarObras").get(0).getMovimientosList());
        assertEquals("Impresionismo",
                ServidoresFalsos.<ListarArtistasRequest>pedidos("CatalogoService.ListarArtistas").get(0).getMovimiento());
        EventosPorMovimientoRequest eventos = ServidoresFalsos
                .<EventosPorMovimientoRequest>pedidos("EventosService.ListarEventosPorMovimiento").get(0);
        assertEquals("Impresionismo", eventos.getMovimiento());
        assertTrue(eventos.getSoloFuturos());
    }

    @Test
    void exploracionDecodificaMovimientosConEspacios() {
        // uri(String) volvería a codificar el %20; con la plantilla el cliente arma el escape una sola vez.
        cliente.get().uri("/api/v2/exploraciones/{m}", "Arte Moderno")
                .header(HttpHeaders.AUTHORIZATION, visitante()).exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.movimiento").isEqualTo("Arte Moderno");
        assertEquals(List.of("Arte Moderno"),
                ServidoresFalsos.<BuscarObrasRequest>pedidos("CatalogoService.BuscarObras").get(0).getMovimientosList());
    }

    @Test
    void categoriasReenviaElCatalogo() {
        getVisitante("/api/v2/categorias").exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.movimientos.length()").isEqualTo(3)
                .jsonPath("$.movimientos[0].nombre").isEqualTo("Impresionismo")
                .jsonPath("$.movimientos[0].cantidadObras").isEqualTo(1)
                .jsonPath("$.tecnicas[0].nombre").isEqualTo("Óleo")
                .jsonPath("$.tematicas[0].cantidadObras").isEqualTo(3);
        assertEquals(List.of("CatalogoService.ListarCategorias"), ServidoresFalsos.LLAMADAS);
    }

    // ---------- Servicio caído ----------

    @Test
    void motorCaidoDa503EnComparacionPeroLaExploracionSigueFuncionando() {
        ServidoresFalsos.motorCaido = true;

        postVisitante("/api/v2/comparaciones").bodyValue("{\"obraIds\":[1,2]}")
                .exchange().expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.codigo").isEqualTo(503)
                .jsonPath("$.estado").isEqualTo("SERVICE_UNAVAILABLE")
                .jsonPath("$.mensaje").isEqualTo("El servicio MotorService no está disponible");

        getVisitante("/api/v2/exploraciones/Impresionismo").exchange().expectStatus().isOk();
    }
}
