package com.unla.museo.controllers;

import com.unla.museo.controllers.util.LinksApi;
import com.unla.museo.dto.request.LoginRequest;
import com.unla.museo.dto.request.UserCreateRequest;
import com.unla.museo.dto.response.LoginResponse;
import com.unla.museo.dto.to.UserTO;
import com.unla.museo.services.impl.JwtServiceImpl;
import com.unla.museo.services.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
public class AuthController {
    private final UserServiceImpl userService;
    private final JwtServiceImpl jwtService;

    public AuthController(UserServiceImpl userService, JwtServiceImpl jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping(value = LinksApi.AuthEndpoints.LOGIN, produces = { "application/json" })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        boolean isValid = userService.validateCredentials(request.email(), request.password());

        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String accessToken = jwtService.generateAccessToken(request.email());
        return ResponseEntity.ok(new LoginResponse(accessToken));

    }

    @PostMapping(value = LinksApi.AuthEndpoints.REGISTER, produces = { "application/json" })
    public ResponseEntity<String> register(@Valid @RequestBody UserCreateRequest request) {
        userService.create(request);
        return ResponseEntity.status(HttpStatus.OK).body("Usuario creado correctamente");
    }


   @Operation(
    summary = "Obtener usuario autenticado",
    security = @SecurityRequirement(name = "bearerAuth")
)
@GetMapping(value = LinksApi.AuthEndpoints.ME, produces = {"application/json"})
public ResponseEntity<UserTO> getUserById(Authentication authentication) {
    String requestEmail = authentication.getName();
    UserTO response = this.userService.getByEmail(requestEmail);
    return ResponseEntity.ok(response);
}

}
