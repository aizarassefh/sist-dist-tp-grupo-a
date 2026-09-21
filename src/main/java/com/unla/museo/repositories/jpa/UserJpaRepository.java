package com.unla.museo.repositories.jpa;

import com.unla.museo.entities.UserEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity,Long> {

    Optional<UserEntity> findById(Long id);
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Transactional
    @Modifying
    @Query("Update UserEntity SET  active = true, creation = :activationDate, password = :password WHERE email = :email ")
    void activateAccount(@Param("email") String email, @Param("activationDate") LocalDateTime activationDate, @Param("password") String password);

}
