package com.unla.museo.seguridad;

import com.unla.museo.seguridad.controller.AutenticacionController;
import com.unla.museo.seguridad.dto.LoginRequest;
import com.unla.museo.seguridad.dto.CrearUsuarioRequest;
import com.unla.museo.seguridad.dto.UsuarioTO;
import com.unla.museo.seguridad.service.jwt.JwtServiceImpl;
import com.unla.museo.seguridad.service.impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacionControllerTest {

    @Mock
    private UsuarioServiceImpl userService;

    @Mock
    private JwtServiceImpl jwtService;

    @Mock
    private Authentication authentication;

    private AutenticacionController controller;

    @BeforeEach
    void setUp() {
        controller = new AutenticacionController(userService, jwtService);
    }

    @Test
    void loginReturnsTokenWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("user@test.com", "Password1!");
        when(userService.validateCredentials(request.email(), request.password())).thenReturn(true);
        when(jwtService.generateAccessToken(request.email())).thenReturn("jwt-token");

        var response = controller.login(request, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jwt-token", response.getBody().accessToken());
        verify(jwtService).generateAccessToken(request.email());
    }

    @Test
    void loginPropagatesBadCredentialsWhenInvalid() {
        LoginRequest request = new LoginRequest("user@test.com", "wrong");
        doThrow(new BadCredentialsException("Email o contraseña incorrectos"))
                .when(userService).validateCredentials(request.email(), request.password());

        assertThrows(BadCredentialsException.class, () -> controller.login(request, null, null));
        verifyNoInteractions(jwtService);
    }

    @Test
    void registerCreatesUserAndReturnsSuccess() {
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        when(userService.create(request)).thenReturn(null);

        var response = controller.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Usuario creado correctamente", response.getBody());
        verify(userService).create(request);
    }

    @Test
    void meReturnsAuthenticatedUser() {
        UsuarioTO user = new UsuarioTO();
        user.setEmail("user@test.com");
        when(authentication.getName()).thenReturn("user@test.com");
        when(userService.getByEmail("user@test.com")).thenReturn(user);

        var response = controller.getUserById(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(user, response.getBody());
    }
}
