package com.unla.museo.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SeguridadTest extends BaseGatewayTest {

    @Test
    void sinTokenDa401ConElCuerpoDeError() {
        cliente.get().uri("/api/v2/categorias").exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo(401)
                .jsonPath("$.estado").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.mensaje").isNotEmpty()
                .jsonPath("$.fechaHora").value(f -> assertTrue(
                        f.toString().matches("\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2}"), "formato de fecha: " + f));
        assertTrue(ServidoresFalsos.LLAMADAS.isEmpty(), "no debe llamar a ningún servicio");
    }

    @Test
    void tokenVencidoAlteradoOBasuraDan401() {
        cliente.get().uri("/api/v2/categorias").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVencido())
                .exchange().expectStatus().isUnauthorized();
        cliente.get().uri("/api/v2/categorias")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFirmadoConOtraClave())
                .exchange().expectStatus().isUnauthorized();
        cliente.get().uri("/api/v2/categorias").header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.es-un-jwt")
                .exchange().expectStatus().isUnauthorized();
        cliente.get().uri("/api/v2/categorias").header(HttpHeaders.AUTHORIZATION, "Basic abc")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void curadorNoPuedeArmarRecorridosNiPedirRecomendaciones() {
        cliente.post().uri("/api/v2/recorridos").header(HttpHeaders.AUTHORIZATION, curador())
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                .exchange().expectStatus().isForbidden()
                .expectBody().jsonPath("$.codigo").isEqualTo(403).jsonPath("$.estado").isEqualTo("FORBIDDEN");
        cliente.get().uri("/api/v2/recomendaciones").header(HttpHeaders.AUTHORIZATION, curador())
                .exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/v2/preferencias").header(HttpHeaders.AUTHORIZATION, curador())
                .exchange().expectStatus().isForbidden();
        assertTrue(ServidoresFalsos.LLAMADAS.isEmpty(), "el 403 debe cortar antes de llamar a los servicios");
    }

    @Test
    void curadorYAdministradorSiPuedenCompararExplorarYVerCategorias() {
        cliente.post().uri("/api/v2/comparaciones").header(HttpHeaders.AUTHORIZATION, curador())
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"obraIds\":[1,2]}")
                .exchange().expectStatus().isOk();
        cliente.get().uri("/api/v2/exploraciones/Impresionismo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("admin@museo.com", "ADMINISTRADOR"))
                .exchange().expectStatus().isOk();
        cliente.get().uri("/api/v2/categorias").header(HttpHeaders.AUTHORIZATION, curador())
                .exchange().expectStatus().isOk();
    }

    @Test
    void unaRutaV2InexistenteDa404EnVezDeCaerEnElProxyDelBackend() {
        // Si el proxy /api/** la capturara, intentaría conectar a localhost:8000 y daría 5xx.
        cliente.get().uri("/api/v2/inexistente").header(HttpHeaders.AUTHORIZATION, visitante())
                .exchange().expectStatus().isNotFound()
                .expectBody().jsonPath("$.codigo").isEqualTo(404);
        cliente.delete().uri("/api/v2/recorridos").header(HttpHeaders.AUTHORIZATION, visitante())
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void elPreflightCorsNoPideToken() {
        // URI absoluta: el filtro CORS compara el origen y necesita esquema y host.
        cliente.method(HttpMethod.OPTIONS).uri("http://localhost/api/v2/recorridos")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type")
                .exchange().expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173");
    }
}
