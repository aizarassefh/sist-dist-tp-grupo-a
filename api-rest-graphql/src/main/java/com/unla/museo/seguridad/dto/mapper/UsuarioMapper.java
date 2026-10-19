package com.unla.museo.seguridad.dto.mapper;

import com.unla.museo.seguridad.dto.CrearUsuarioRequest;
import com.unla.museo.seguridad.dto.UsuarioTO;
import com.unla.museo.seguridad.entity.RolEntity;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UsuarioMapper {

    public UsuarioEntity toEntity(CrearUsuarioRequest request, RolEntity rol, String creadoPor) {
        if (request == null) {
            return null;
        }

        UsuarioEntity entity = new UsuarioEntity();
        entity.setEmail(request.getEmail());
        entity.setNombre(request.getNombre());
        entity.setApellido(request.getApellido());
        entity.setRol(rol);
        entity.setCreadoPor(creadoPor);
        entity.setCreacion(LocalDateTime.now());
        entity.setActivo(true);
        return entity;
    }

    public UsuarioTO toResponse(UsuarioEntity entity) {
        if (entity == null) {
            return null;
        }

        UsuarioTO response = new UsuarioTO();
        response.setId(entity.getId());
        response.setEmail(entity.getEmail());
        response.setNombre(entity.getNombre());
        response.setApellido(entity.getApellido());
        response.setTelefono(entity.getTelefono());
        response.setRol(entity.getRol() != null ? entity.getRol().getNombre() : null);
        return response;
    }
}
