package com.example.demologin.repository;

import com.example.demologin.entity.CalendarCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CalendarCredentialRepository extends JpaRepository<CalendarCredential, Long> {
    Optional<CalendarCredential> findByUserIdAndProvider(Long userId, String provider);
}
