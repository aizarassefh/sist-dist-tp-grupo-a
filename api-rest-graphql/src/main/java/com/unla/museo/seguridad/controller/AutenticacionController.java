package com.unla.museo.seguridad.controller;

import com.unla.museo.seguridad.util.LinksApi;
import com.unla.museo.seguridad.dto.LoginRequest;
import com.unla.museo.seguridad.dto.CrearUsuarioRequest;
import com.unla.museo.seguridad.dto.LoginResponse;
import com.unla.museo.seguridad.dto.UsuarioTO;
import com.unla.museo.seguridad.service.jwt.JwtServiceImpl;
import com.unla.museo.seguridad.service.impl.UsuarioServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Autenticación", description = "Registro, login y usuario autenticado")
public class AutenticacionController {
    private final UsuarioServiceImpl userService;
    private final JwtServiceImpl jwtService;

    public AutenticacionController(UsuarioServiceImpl userService, JwtServiceImpl jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Operation(summary = "Iniciar sesión y obtener un token JWT")
    @PostMapping(value = LinksApi.AuthEndpoints.LOGIN, produces = { "application/json" })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        // Se normaliza una sola vez acá y se usa el mismo valor para validar
        // credenciales y para generar el token: si no, un email con mayúsculas
        // pasa la validación (que normaliza internamente) pero el token
        // buscaría al usuario con el email crudo y fallaría.
        String email = request.email() == null ? null : request.email().strip().toLowerCase();
        userService.validateCredentials(email, request.password());
        String accessToken = jwtService.generateAccessToken(email);
        return ResponseEntity.ok(new LoginResponse(accessToken, "Bearer", jwtService.duracionEnSegundos()));
    }

    @Operation(summary = "Registrar un nuevo usuario visitante")
    @PostMapping(value = LinksApi.AuthEndpoints.REGISTER, produces = { "application/json" })
    public ResponseEntity<String> register(@Valid @RequestBody CrearUsuarioRequest request) {
        userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Usuario creado correctamente");
    }


   @Operation(
    summary = "Obtener usuario autenticado",
    security = @SecurityRequirement(name = "bearerAuth")
)
@GetMapping(value = LinksApi.AuthEndpoints.ME, produces = {"application/json"})
public ResponseEntity<UsuarioTO> getUserById(Authentication authentication) {
    String requestEmail = authentication.getName();
    UsuarioTO response = this.userService.getByEmail(requestEmail);
    return ResponseEntity.ok(response);
}

}
