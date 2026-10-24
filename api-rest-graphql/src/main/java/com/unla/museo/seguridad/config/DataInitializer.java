package com.unla.museo.seguridad.config;

import com.unla.museo.seguridad.repository.UsuarioRepository;
import com.unla.museo.seguridad.entity.RolEntity;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.seguridad.repository.RolRepository;
import com.unla.museo.seguridad.util.Roles;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    // Los roles hacen falta siempre (sin ellos no se puede ni loguear un
    // usuario existente en una base ya poblada). Los datos de ejemplo, no.
    @Bean
    @Order(1)
    CommandLineRunner initRoles(RolRepository rolRepository) {
        return args -> {
            crearRolSiNoExiste(rolRepository, Roles.VISITANTE, "Visitante", "Usuario visitante del museo");
            crearRolSiNoExiste(rolRepository, Roles.CURADOR, "Curador", "Usuario encargado de la gestión del museo");
            crearRolSiNoExiste(rolRepository, Roles.ADMIN, "Administrador", "Usuario administrador del sistema");
        };
    }

    // @Order(2): los eventos de ejemplo (EventosDataInitializer) necesitan que
    // estos usuarios ya existan, para asignarlos como curadores e inscriptos.
    @Bean
    @Order(2)
    @ConditionalOnProperty(name = "app.datos-de-ejemplo", havingValue = "true", matchIfMissing = true)
    CommandLineRunner initUsuariosDeEjemplo(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            crearUsuarioSiNoExiste(usuarioRepository, rolRepository, passwordEncoder,
                    "visitante@test.com", "Carlos", "Hernandez", "1100000001", Roles.VISITANTE);
            crearUsuarioSiNoExiste(usuarioRepository, rolRepository, passwordEncoder,
                    "curador@test.com", "Juan", "Perez", "1100000000", Roles.CURADOR);
            crearUsuarioSiNoExiste(usuarioRepository, rolRepository, passwordEncoder,
                    "admin@test.com", "Nacho", "Medina", "1100000002", Roles.ADMIN);
            crearUsuarioSiNoExiste(usuarioRepository, rolRepository, passwordEncoder,
                    "marta.gomez@test.com", "Marta", "Gómez", "1100000003", Roles.CURADOR);

            crearUsuarioSiNoExiste(usuarioRepository, rolRepository, passwordEncoder,
                    "maria.gonzalez@test.com", "María", "González", "1100000004", Roles.VISITANTE);
            crearUsuarioSiNoExiste(usuarioRepository, rolRepository, passwordEncoder,
                    "lucia.fernandez@test.com", "Lucía", "Fernández", "1100000005", Roles.VISITANTE);
            crearUsuarioSiNoExiste(usuarioRepository, rolRepository, passwordEncoder,
                    "martin.rodriguez@test.com", "Martín", "Rodríguez", "1100000006", Roles.VISITANTE);
            crearUsuarioSiNoExiste(usuarioRepository, rolRepository, passwordEncoder,
                    "sofia.lopez@test.com", "Sofía", "López", "1100000007", Roles.VISITANTE);
            crearUsuarioSiNoExiste(usuarioRepository, rolRepository, passwordEncoder,
                    "nicolas.diaz@test.com", "Nicolás", "Díaz", "1100000008", Roles.VISITANTE);
            crearUsuarioSiNoExiste(usuarioRepository, rolRepository, passwordEncoder,
                    "valentina.martinez@test.com", "Valentina", "Martínez", "1100000009", Roles.VISITANTE);
            crearUsuarioSiNoExiste(usuarioRepository, rolRepository, passwordEncoder,
                    "federico.sanchez@test.com", "Federico", "Sánchez", "1100000010", Roles.VISITANTE);
            crearUsuarioSiNoExiste(usuarioRepository, rolRepository, passwordEncoder,
                    "camila.romero@test.com", "Camila", "Romero", "1100000011", Roles.VISITANTE);
        };
    }

    private void crearRolSiNoExiste(
            RolRepository rolRepository,
            String id,
            String name,
            String description) {

        if (rolRepository.findById(id).isEmpty()) {

            RolEntity role = new RolEntity();

            role.setId(id);
            role.setNombre(name);
            role.setDescripcion(description);

            rolRepository.save(role);
        }
    }

    private void crearUsuarioSiNoExiste(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String nombre,
            String apellido,
            String telefono,
            String rolId) {

        if (usuarioRepository.findByEmail(email).isEmpty()) {

            RolEntity rol = rolRepository.findById(rolId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "No existe el rol: " + rolId
                            )
                    );

            UsuarioEntity user = new UsuarioEntity();

            user.setEmail(email);
            user.setNombre(nombre);
            user.setApellido(apellido);
            user.setTelefono(telefono);
            user.setActivo(true);
            user.setRol(rol);
            user.setPassword(
                    passwordEncoder.encode("Aa@12345678")
            );
            user.setCreacion(LocalDateTime.now());

            usuarioRepository.save(user);
        }
    }
}
