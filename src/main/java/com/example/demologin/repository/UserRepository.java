package com.example.demologin.repository;

import com.example.demologin.entity.User;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByEmailAndUserIdNot(String email, Long userId);


    Page<User> findByRoles_Name(String roleName, Pageable pageable);

    boolean existsByRoles_Id(Long id);
}
