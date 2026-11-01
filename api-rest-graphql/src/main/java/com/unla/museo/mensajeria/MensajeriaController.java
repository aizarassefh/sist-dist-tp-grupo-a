package com.unla.museo.mensajeria;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mensajeria")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mensajería", description = "Estado de la mensajería con Kafka y mensajes fallidos")
@PreAuthorize("hasAnyRole('CURADOR','ADMINISTRADOR')")
public class MensajeriaController {

    private final MensajeriaService servicio;

    public MensajeriaController(MensajeriaService servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Comparar lo publicado con lo procesado por cada consumidor")
    @GetMapping("/consistencia")
    public ConsistenciaDTO consistencia() {
        return servicio.consistencia();
    }

    @Operation(summary = "Listar mensajes que terminaron en el DLT")
    @GetMapping("/fallidos")
    public List<FallidoDTO> fallidos(@RequestParam(required = false) EstadoFallido estado) {
        return servicio.listarFallidos(estado);
    }

    @Operation(summary = "Volver a publicar un mensaje fallido")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/fallidos/{id}/reprocesar")
    public ResponseEntity<Void> reprocesar(@PathVariable Long id) {
        servicio.reprocesar(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
