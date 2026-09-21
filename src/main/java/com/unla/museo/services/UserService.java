package com.unla.museo.services;

import com.unla.museo.dto.request.UserCreateRequest;
import com.unla.museo.dto.to.UserTO;


public interface UserService {

    UserTO create(UserCreateRequest request);

    boolean validateCredentials(String username, String rawPassword);

    UserTO getById(long id);

}
