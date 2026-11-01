package com.unla.museo.auditoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unla.museo.mensajeria.NovedadTipo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria/eventos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Auditoría", description = "Historial de altas, modificaciones y cancelaciones de eventos")
@PreAuthorize("hasAnyRole('CURADOR','ADMINISTRADOR')")
public class HistorialActividadController {

    private static final int LIMITE_MAXIMO = 500;

    private final HistorialActividadRepository repository;
    private final ObjectMapper objectMapper;

    public HistorialActividadController(HistorialActividadRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "Historial de actividad sobre eventos, el más reciente primero")
    @GetMapping
    public List<HistorialActividadDTO> listar(@RequestParam(required = false) Long idEvento,
                                              @RequestParam(required = false) NovedadTipo tipo,
                                              @RequestParam(defaultValue = "100") int limite) {
        PageRequest pagina = PageRequest.of(0, Math.max(1, Math.min(limite, LIMITE_MAXIMO)));
        return repository.buscar(idEvento, tipo != null ? tipo.name() : null, pagina).stream()
                .map(h -> HistorialActividadDTO.desde(h, objectMapper))
                .toList();
    }
}
