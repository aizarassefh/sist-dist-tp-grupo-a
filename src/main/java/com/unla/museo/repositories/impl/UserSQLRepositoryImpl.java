package com.unla.museo.repositories.impl;

import com.unla.museo.entities.UserEntity;
import com.unla.museo.repositories.UserRepository;
import com.unla.museo.repositories.jpa.UserJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Qualifier("UserSQLRepositoryImpl")
@Repository
@AllArgsConstructor
public class UserSQLRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        return userJpaRepository.findByEmail(email);
    }
    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public UserEntity save(UserEntity userEntity) {
        return userJpaRepository.save(userEntity);
    }


    @Override
    public Optional<UserEntity> findById(Long id) {
        return this.userJpaRepository.findById(id);
    }


}
