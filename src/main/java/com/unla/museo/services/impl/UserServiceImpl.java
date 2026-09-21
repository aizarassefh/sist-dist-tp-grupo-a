package com.unla.museo.services.impl;


import com.unla.museo.dto.request.UserCreateRequest;
import com.unla.museo.dto.to.UserTO;
import com.unla.museo.entities.RoleEntity;
import com.unla.museo.entities.UserEntity;
import com.unla.museo.exception.ErrorMessage;
import com.unla.museo.exception.ResourceNotFoundException;
import com.unla.museo.exception.UserAlreadyExistsException;
import com.unla.museo.exception.UserNotFoundException;
import com.unla.museo.helper.mapper.UserMapper;
import com.unla.museo.repositories.RoleRepository;
import com.unla.museo.repositories.UserRepository;
import com.unla.museo.services.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Qualifier("UserServiceImpl")
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    public UserServiceImpl(
            @Qualifier("UserSQLRepositoryImpl") UserRepository userRepository,
            @Qualifier("RoleSQLRepositoryImpl") RoleRepository roleRepository,
           PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Override
    public UserTO create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(ErrorMessage.User.CONFLICT_EMAIL);
        }
        RoleEntity roleEntity = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.User.Role.NOT_FOUND));
        UserEntity user = userMapper.toEntity(request,roleEntity,request.getEmail());
        user.setPassword(this.passwordEncoder.encode(request.getPassword()));
        user.setCreation(LocalDateTime.now());
        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }


    @Override
    public boolean validateCredentials(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .map(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .orElseThrow(() -> new BadCredentialsException(ErrorMessage.AUTH_FAILED));
    }

    public UserTO getById(long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(ErrorMessage.User.NOT_FOUND));
        return userMapper.toResponse(user);
    }

}
