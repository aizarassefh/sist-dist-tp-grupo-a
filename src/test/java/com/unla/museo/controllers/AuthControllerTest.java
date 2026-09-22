package com.unla.museo.controllers;

import com.unla.museo.dto.request.LoginRequest;
import com.unla.museo.dto.request.UserCreateRequest;
import com.unla.museo.dto.to.UserTO;
import com.unla.museo.services.impl.JwtServiceImpl;
import com.unla.museo.services.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserServiceImpl userService;

    @Mock
    private JwtServiceImpl jwtService;

    @Mock
    private Authentication authentication;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(userService, jwtService);
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
    void loginReturnsUnauthorizedWhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest("user@test.com", "wrong");
        when(userService.validateCredentials(request.email(), request.password())).thenReturn(false);

        var response = controller.login(request, null, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(jwtService);
    }

    @Test
    void registerCreatesUserAndReturnsSuccess() {
        UserCreateRequest request = new UserCreateRequest();
        when(userService.create(request)).thenReturn(null);

        var response = controller.register(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Usuario creado correctamente", response.getBody());
        verify(userService).create(request);
    }

    @Test
    void meReturnsAuthenticatedUser() {
        UserTO user = new UserTO();
        user.setEmail("user@test.com");
        when(authentication.getName()).thenReturn("user@test.com");
        when(userService.getByEmail("user@test.com")).thenReturn(user);

        var response = controller.getUserById(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(user, response.getBody());
    }
}
