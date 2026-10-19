package com.unla.museo.eventos.controller;

import com.unla.museo.eventos.util.EstadoEvento;
import com.unla.museo.eventos.service.EventoService;
import com.unla.museo.eventos.util.TipoEvento;
import com.unla.museo.eventos.dto.EventoDTO;
import com.unla.museo.eventos.dto.EventoRequest;
import com.unla.museo.eventos.dto.ModificarCupoRequest;
import com.unla.museo.eventos.dto.PaginaEventosDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/eventos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Eventos", description = "Alta, edición, baja, listado e inscripciones a eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @Operation(summary = "Listar eventos con filtros y paginado")
    @GetMapping
    public ResponseEntity<PaginaEventosDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) TipoEvento tipo,
            @RequestParam(required = false) Long curadorId,
            @RequestParam(required = false) EstadoEvento estado,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanio,
            Authentication authentication) {
        return ResponseEntity.ok(eventoService.listar(desde, hasta, tipo, curadorId, estado, pagina, tamanio, authentication));
    }

    @Operation(summary = "Ver el detalle de un evento")
    @GetMapping("/{id}")
    public ResponseEntity<EventoDTO> obtener(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(eventoService.obtenerDetalle(id, authentication));
    }

    @Operation(summary = "Crear un evento")
    @PreAuthorize("hasAnyRole('CURADOR','ADMINISTRADOR')")
    @PostMapping
    public ResponseEntity<EventoDTO> crear(@Valid @RequestBody EventoRequest request) {
        EventoDTO creado = eventoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(summary = "Editar un evento")
    @PreAuthorize("hasAnyRole('CURADOR','ADMINISTRADOR')")
    @PutMapping("/{id}")
    public ResponseEntity<EventoDTO> editar(@PathVariable Long id, @Valid @RequestBody EventoRequest request) {
        return ResponseEntity.ok(eventoService.editar(id, request));
    }

    @Operation(summary = "Borrar un evento (borra en cascada sus inscripciones)")
    @PreAuthorize("hasAnyRole('CURADOR','ADMINISTRADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable Long id) {
        eventoService.borrar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Inscribirse a un evento")
    @PostMapping("/{id}/inscripcion")
    public ResponseEntity<EventoDTO> inscribirse(@PathVariable Long id, Authentication authentication) {
        eventoService.inscribirse(id, authentication);
        EventoDTO evento = eventoService.obtenerDetalle(id, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(evento);
    }

    @Operation(summary = "Desinscribirse de un evento")
    @DeleteMapping("/{id}/inscripcion")
    public ResponseEntity<Void> desinscribirse(@PathVariable Long id, Authentication authentication) {
        eventoService.desinscribirse(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Modificar únicamente el cupo máximo de un evento")
    @PreAuthorize("hasAnyRole('CURADOR','ADMINISTRADOR')")
    @PatchMapping("/{id}/cupo")
    public ResponseEntity<EventoDTO> modificarCupo(
            @PathVariable Long id, 
            @Valid @RequestBody ModificarCupoRequest request) {
        
        // Extraemos el int del record y llamamos al servicio intacto
        return ResponseEntity.ok(eventoService.modificarCupo(id, request.cupoMaximo()));
    }
}
