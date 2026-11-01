package com.unla.museo.novedades;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/novedades")
public class NovedadController {
    private final NovedadRepository repository;
    public NovedadController(NovedadRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<NovedadDTO> listar(Authentication authentication,
                                   @RequestParam(defaultValue = "20") int limite) {
        int limiteSeguro = Math.max(1, Math.min(limite, 100));
        return repository.findAllByOrderByOccurredAtDesc(PageRequest.of(0, limiteSeguro))
                .stream().map(NovedadDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<NovedadDTO> obtener(@PathVariable UUID id, Authentication authentication) {
        return repository.findById(id)
                .map(novedad -> ResponseEntity.ok(NovedadDTO.desde(novedad)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
