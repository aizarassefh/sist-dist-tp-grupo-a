package com.unla.museo.helper.mapper;

import com.unla.museo.dto.to.RoleTO;
import com.unla.museo.entities.RoleEntity;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public RoleTO toResponse(RoleEntity entity) {
        if (entity == null) {
            return null;
        }

        RoleTO response = new RoleTO();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        return response;
    }
}
