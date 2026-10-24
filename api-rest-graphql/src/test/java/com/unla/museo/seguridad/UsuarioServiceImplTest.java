package com.unla.museo.seguridad;

import com.unla.museo.comun.errores.RecursoInexistenteException;
import com.unla.museo.seguridad.dto.CrearUsuarioRequest;
import com.unla.museo.seguridad.dto.UsuarioTO;
import com.unla.museo.seguridad.dto.mapper.UsuarioMapper;
import com.unla.museo.seguridad.entity.RolEntity;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.seguridad.exception.UsuarioExistenteException;
import com.unla.museo.seguridad.exception.UsuarioNoEncontradoException;
import com.unla.museo.seguridad.repository.RolRepository;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import com.unla.museo.seguridad.service.impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UsuarioServiceImpl service;
    private final UsuarioMapper usuarioMapper = new UsuarioMapper();

    @BeforeEach
    void setUp() {
        service = new UsuarioServiceImpl(usuarioRepository, rolRepository, passwordEncoder, usuarioMapper);
    }

    @Test
    void createPersistsUserWithEncodedPasswordAndRole() {
        CrearUsuarioRequest request = validRequest();
        RolEntity role = role("CURADOR", "Curador");
        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(rolRepository.findById(anyString())).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioTO result = service.create(request);

        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("encoded", captor.getValue().getPassword());
        assertSame(role, captor.getValue().getRol());
        assertEquals(request.getEmail(), result.getEmail());
    }

    @Test
    void createRejectsDuplicatedEmail() {
        CrearUsuarioRequest request = validRequest();
        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(UsuarioExistenteException.class, () -> service.create(request));
        verifyNoInteractions(rolRepository, passwordEncoder);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownRole() {
        CrearUsuarioRequest request = validRequest();
        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(rolRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(RecursoInexistenteException.class, () -> service.create(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void createNormalizesEmailWithUppercaseAndSpaces() {
        CrearUsuarioRequest request = validRequest();
        request.setEmail("  Ana@Test.com  ");
        RolEntity role = role("VISITANTE", "Visitante");
        when(usuarioRepository.existsByEmail("ana@test.com")).thenReturn(false);
        when(rolRepository.findById(anyString())).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioTO result = service.create(request);

        assertEquals("ana@test.com", result.getEmail());
        verify(usuarioRepository).existsByEmail("ana@test.com");
    }

    @Test
    void validateCredentialsReturnsTrueForMatchingPassword() {
        UsuarioEntity user = user("user@test.com", "encoded");
        when(usuarioRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1!", "encoded")).thenReturn(true);

        assertTrue(service.validateCredentials(user.getEmail(), "Password1!"));
    }

    @Test
    void validateCredentialsNormalizesEmailWithUppercaseAndSpaces() {
        UsuarioEntity user = user("ana@test.com", "encoded");
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1!", "encoded")).thenReturn(true);

        assertTrue(service.validateCredentials("  Ana@Test.com  ", "Password1!"));
    }

    @Test
    void validateCredentialsRejectsWrongPasswordWithGenericMessage() {
        UsuarioEntity user = user("user@test.com", "encoded");
        when(usuarioRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> service.validateCredentials(user.getEmail(), "wrong"));
        assertEquals("Email o contraseña incorrectos", ex.getMessage());
    }

    @Test
    void validateCredentialsRejectsUnknownUserWithSameGenericMessage() {
        when(usuarioRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> service.validateCredentials("missing@test.com", "Password1!"));
        assertEquals("Email o contraseña incorrectos", ex.getMessage());
    }

    @Test
    void getByEmailReturnsMappedUser() {
        UsuarioEntity user = user("user@test.com", "encoded");
        user.setRol(role("CURADOR", "Curador"));
        when(usuarioRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        UsuarioTO result = service.getByEmail(user.getEmail());

        assertEquals(user.getEmail(), result.getEmail());
        assertEquals("Curador", result.getRol());
    }

    @Test
    void getByEmailRejectsUnknownUser() {
        when(usuarioRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> service.getByEmail("missing@test.com"));
    }

    private CrearUsuarioRequest validRequest() {
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setEmail("user@test.com");
        request.setNombre("Test");
        request.setApellido("User");
        request.setTelefono("123");
        request.setPassword("Password1!");
        return request;
    }

    private UsuarioEntity user(String email, String password) {
        UsuarioEntity user = new UsuarioEntity();
        user.setEmail(email);
        user.setPassword(password);
        return user;
    }

    private RolEntity role(String id, String nombre) {
        RolEntity role = new RolEntity();
        role.setId(id);
        role.setNombre(nombre);
        return role;
    }
}
