package com.unla.museo.helper.mapper;

import com.unla.museo.dto.request.UserCreateRequest;
import com.unla.museo.dto.to.UserTO;
import com.unla.museo.entities.RoleEntity;
import com.unla.museo.entities.UserEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserMapper {

    public UserEntity toEntity(UserCreateRequest request, RoleEntity role, String createdBy) {
        if (request == null) {
            return null;
        }

        UserEntity entity = new UserEntity();
        entity.setEmail(request.getEmail());
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setRole(role);
        entity.setCreatedBy(createdBy);
        entity.setCreation(LocalDateTime.now());
        entity.setActive(true);
        return entity;
    }

    public UserTO toResponse(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        UserTO response = new UserTO();
        response.setId(entity.getId());
        response.setEmail(entity.getEmail());
        response.setFirstName(entity.getFirstName());
        response.setLastName(entity.getLastName());
        response.setPhoneNumber(entity.getPhoneNumber());
        response.setRole(entity.getRole() != null ? entity.getRole().getName() : null);
        return response;
    }
}
