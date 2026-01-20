package com.example.demologin.repository;

import com.example.demologin.entity.RefreshToken;
import com.example.demologin.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUser(User user);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user")
    void deleteByUser(@Param("user") User user);

    int deleteByExpiryDateBefore(LocalDateTime now);

    Optional<RefreshToken> findTopByUserOrderByExpiryDateDesc(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.jti = :jti")
    int deleteByJti(@Param("jti") String jti);

    boolean existsByJti(String jti);
}
