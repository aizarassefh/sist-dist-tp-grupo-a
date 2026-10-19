package com.unla.museo.seguridad.service;

import com.unla.museo.seguridad.dto.CrearUsuarioRequest;
import com.unla.museo.seguridad.dto.PersonaTO;
import com.unla.museo.seguridad.dto.UsuarioTO;

import java.util.List;


public interface UsuarioService {

    UsuarioTO create(CrearUsuarioRequest request);

    boolean validateCredentials(String username, String rawPassword);

    UsuarioTO getByEmail(String email);

    List<PersonaTO> getCuradores();

}
