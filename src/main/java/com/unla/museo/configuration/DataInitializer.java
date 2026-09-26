package com.unla.museo.configuration;

import com.unla.museo.constants.Roles;
import com.unla.museo.entities.RoleEntity;
import com.unla.museo.entities.UserEntity;
import com.unla.museo.repositories.RoleRepository;
import com.unla.museo.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsers(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            // ==========================================
            // CREAR ROLES
            // ==========================================

            crearRolSiNoExiste(
                    roleRepository,
                    Roles.VISITANTE,
                    "Visitante",
                    "Usuario visitante del museo"
            );

            crearRolSiNoExiste(
                    roleRepository,
                    Roles.CURADOR,
                    "Curador",
                    "Usuario encargado de la gestión del museo"
            );

            crearRolSiNoExiste(
                    roleRepository,
                    Roles.ADMIN,
                    "Administrador",
                    "Usuario administrador del sistema"
            );

            // ==========================================
            // CREAR USUARIOS
            // ==========================================

            crearUsuarioSiNoExiste(
                    userRepository,
                    roleRepository,
                    passwordEncoder,
                    "visitante@test.com",
                    "Carlos",
                    "Hernandez",
                    "1100000001",
                    Roles.VISITANTE
            );

            crearUsuarioSiNoExiste(
                    userRepository,
                    roleRepository,
                    passwordEncoder,
                    "curador@test.com",
                    "Juan",
                    "Perez",
                    "1100000000",
                    Roles.CURADOR
            );

            crearUsuarioSiNoExiste(
                    userRepository,
                    roleRepository,
                    passwordEncoder,
                    "admin@test.com",
                    "Nacho",
                    "Medina",
                    "1100000002",
                    Roles.ADMIN
            );
        };
    }

    private void crearRolSiNoExiste(
            RoleRepository roleRepository,
            String id,
            String name,
            String description) {

        if (roleRepository.findById(id).isEmpty()) {

            RoleEntity role = new RoleEntity();

            role.setId(id);
            role.setName(name);
            role.setDescription(description);

            roleRepository.save(role);
        }
    }

    private void crearUsuarioSiNoExiste(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String firstName,
            String lastName,
            String phoneNumber,
            String roleId) {

        if (userRepository.findByEmail(email).isEmpty()) {

            RoleEntity role = roleRepository.findById(roleId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "No existe el rol: " + roleId
                            )
                    );

            UserEntity user = new UserEntity();

            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPhoneNumber(phoneNumber);
            user.setActive(true);
            user.setRole(role);
            user.setPassword(
                    passwordEncoder.encode("Aa@12345678")
            );
            user.setCreation(LocalDateTime.now());

            userRepository.save(user);
        }
    }
}