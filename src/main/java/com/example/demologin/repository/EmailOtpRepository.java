package com.example.demologin.repository;

import com.example.demologin.entity.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {
    Optional<EmailOtp> findTopByEmailAndTypeOrderByCreatedAtDesc(String email, String type);
    List<EmailOtp> findByEmailAndTypeAndVerifiedFalse(String email, String type);
    void deleteByEmailAndType(String email, String type);
    
    @Modifying
    @Query("DELETE FROM EmailOtp e WHERE e.expiredAt < :currentTime")
    int deleteExpiredOtps(LocalDateTime currentTime);
} 
