package com.example.demologin.serviceImpl;

import com.example.demologin.dto.response.TokenRefreshResponse;
import com.example.demologin.entity.RefreshToken;
import com.example.demologin.entity.User;
import com.example.demologin.exception.exceptions.TokenRefreshException;
import com.example.demologin.repository.RefreshTokenRepository;
import com.example.demologin.service.RefreshTokenService;
import com.example.demologin.service.TokenService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    @Value("${jwt.refresh.expiration.ms}")
    private Long refreshTokenDurationMs;


    private final RefreshTokenRepository refreshTokenRepository;

    private final TokenService tokenService;

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);

        long expirySeconds = refreshTokenDurationMs / 1000;
        refreshToken.setExpiryDate(
                LocalDateTime.now().plusSeconds(expirySeconds)
        );
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setJti(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }



    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(
                    token.getToken(),
                    "Refresh token was expired. Please make a new login request"
            );
        }
        return token;
    }



    @Override
    public TokenRefreshResponse refreshToken(String requestRefreshToken) {
        return findByToken(requestRefreshToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = tokenService.generateTokenForUser(user);
                    return new TokenRefreshResponse(token, requestRefreshToken);
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token is not in database!"));
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = refreshTokenRepository.deleteByExpiryDateBefore(now);
        if (deletedCount > 0) {
            log.info("✅ Đã xóa {} refresh token hết hạn trước {}", deletedCount, now);
        }
    }

} 
