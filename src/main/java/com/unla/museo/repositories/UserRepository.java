package com.unla.museo.repositories;

import com.unla.museo.entities.UserEntity;
import java.util.Optional;


public interface UserRepository{

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    UserEntity save(UserEntity userEntity);

    Optional<UserEntity> findById(Long id);

}
