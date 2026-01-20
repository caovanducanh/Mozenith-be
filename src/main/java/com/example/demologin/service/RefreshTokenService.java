package com.example.demologin.service;

import com.example.demologin.dto.response.TokenRefreshResponse;
import com.example.demologin.entity.RefreshToken;
import com.example.demologin.entity.User;

import java.util.Optional;

public interface RefreshTokenService {
    Optional<RefreshToken> findByToken(String token);
    RefreshToken createRefreshToken(User user);
    RefreshToken verifyExpiration(RefreshToken token);
    TokenRefreshResponse refreshToken(String requestRefreshToken);
}
