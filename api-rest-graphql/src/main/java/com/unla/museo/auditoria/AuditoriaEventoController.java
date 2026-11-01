package com.unla.museo.auditoria;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria/eventos")
@PreAuthorize("hasAnyRole('CURADOR', 'ADMINISTRADOR')")
public class AuditoriaEventoController {
    private final AuditoriaEventoRepository repository;

    public AuditoriaEventoController(AuditoriaEventoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AuditoriaEventoDTO> listar(@RequestParam(defaultValue = "50") int limite) {
        int limiteSeguro = Math.max(1, Math.min(limite, 200));
        return repository.findAllByOrderByOccurredAtDesc(PageRequest.of(0, limiteSeguro))
                .stream().map(AuditoriaEventoDTO::desde).toList();
    }
}
