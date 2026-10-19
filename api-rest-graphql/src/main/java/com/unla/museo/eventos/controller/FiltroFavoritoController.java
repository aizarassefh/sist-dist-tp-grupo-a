package com.unla.museo.eventos.controller;

import com.unla.museo.eventos.service.FiltroFavoritoService;
import com.unla.museo.eventos.dto.FiltroFavoritoDTO;
import com.unla.museo.eventos.dto.FiltroFavoritoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/filtros-favoritos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Filtros favoritos", description = "Filtros de eventos guardados por cada usuario")
public class FiltroFavoritoController {

    private final FiltroFavoritoService filtroFavoritoService;

    public FiltroFavoritoController(FiltroFavoritoService filtroFavoritoService) {
        this.filtroFavoritoService = filtroFavoritoService;
    }

    @Operation(summary = "Listar los filtros favoritos del usuario autenticado")
    @GetMapping
    public ResponseEntity<List<FiltroFavoritoDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(filtroFavoritoService.listar(authentication));
    }

    @Operation(summary = "Crear un filtro favorito")
    @PostMapping
    public ResponseEntity<FiltroFavoritoDTO> crear(@Valid @RequestBody FiltroFavoritoRequest request,
                                                     Authentication authentication) {
        FiltroFavoritoDTO creado = filtroFavoritoService.crear(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(summary = "Editar un filtro favorito propio")
    @PutMapping("/{id}")
    public ResponseEntity<FiltroFavoritoDTO> editar(@PathVariable Long id,
                                                      @Valid @RequestBody FiltroFavoritoRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(filtroFavoritoService.editar(id, request, authentication));
    }

    @Operation(summary = "Borrar un filtro favorito propio")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable Long id, Authentication authentication) {
        filtroFavoritoService.borrar(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
