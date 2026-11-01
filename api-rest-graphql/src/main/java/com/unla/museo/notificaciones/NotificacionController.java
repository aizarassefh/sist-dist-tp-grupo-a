package com.unla.museo.notificaciones;

import com.unla.museo.comun.errores.RecursoInexistenteException;
import com.unla.museo.seguridad.service.UsuarioActualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Siempre las notificaciones del usuario del token: el id nunca viene del cliente. */
@RestController
@RequestMapping("/api/notificaciones")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notificaciones", description = "Avisos de eventos para el visitante")
@PreAuthorize("hasRole('VISITANTE')")
public class NotificacionController {

    private static final int LIMITE_MAXIMO = 200;

    private final NotificacionRepository repository;
    private final UsuarioActualService usuarioActualService;

    public NotificacionController(NotificacionRepository repository, UsuarioActualService usuarioActualService) {
        this.repository = repository;
        this.usuarioActualService = usuarioActualService;
    }

    @Operation(summary = "Notificaciones del usuario, las más recientes primero")
    @GetMapping
    public NotificacionesDTO listar(@RequestParam(defaultValue = "false") boolean soloNoLeidas,
                                    @RequestParam(defaultValue = "50") int limite,
                                    Authentication authentication) {
        Long idUsuario = idUsuario(authentication);
        PageRequest pagina = PageRequest.of(0, Math.max(1, Math.min(limite, LIMITE_MAXIMO)));
        List<NotificacionEntity> filas = soloNoLeidas
                ? repository.findByIdUsuarioAndLeidaFalseOrderByFechaOperacionDescIdDesc(idUsuario, pagina)
                : repository.findByIdUsuarioOrderByFechaOperacionDescIdDesc(idUsuario, pagina);
        return new NotificacionesDTO(repository.countByIdUsuarioAndLeidaFalse(idUsuario),
                filas.stream().map(NotificacionesDTO.NotificacionDTO::desde).toList());
    }

    @Operation(summary = "Marcar una notificación como leída")
    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id, Authentication authentication) {
        if (repository.marcarLeida(id, idUsuario(authentication)) == 0) {
            throw new RecursoInexistenteException("La notificación no existe");
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Marcar todas las notificaciones como leídas")
    @PatchMapping("/leidas")
    public ResponseEntity<Void> marcarTodasLeidas(Authentication authentication) {
        repository.marcarTodasLeidas(idUsuario(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long idUsuario(Authentication authentication) {
        return usuarioActualService.obtenerPorEmail(authentication.getName()).getId();
    }
}
