package com.example.demologin.utils;

import com.example.demologin.entity.RefreshToken;
import com.example.demologin.entity.User;
import com.example.demologin.exception.exceptions.TokenValidationException;
import com.example.demologin.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * JWT Utility class for token operations
 * Handles JWT creation, validation, and extraction
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration.ms}")
    private int jwtExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();
    /**
     * Get signing key for JWT
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate JWT token for user
     */
    // In JwtUtil.java
    public String generateToken(User user) {
        RefreshToken latestRefreshToken = refreshTokenRepository
                .findTopByUserOrderByExpiryDateDesc(user)
                .orElse(null);

        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenVersion", user.getTokenVersion());

        // Only store role names, not full objects
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());
        claims.put("roles", roleNames);

        claims.put("fullName", user.getFullName());

        if (latestRefreshToken != null) {
            claims.put("jti", latestRefreshToken.getJti());

            // Truyền luôn expiryDate refresh token (dạng epoch millis cho frontend dễ xử lý)
            claims.put("refreshExp", latestRefreshToken.getExpiryDate()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli());
        }

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(user.getUserId()))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs)) // access token exp
                .signWith(getSigningKey())
                .compact();
    }



    public Set<String> extractRoles(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object rolesObj = claims.get("roles");

            if (rolesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> rolesList = (List<String>) rolesObj;
                return new HashSet<>(rolesList);
            } else if (rolesObj instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<String> rolesSet = (Set<String>) rolesObj;
                return rolesSet;
            } else {
                log.warn("Invalid role format in token for user: {}", claims.getSubject());
                throw new TokenValidationException("Invalid role format in token",
                        TokenValidationException.TokenErrorType.MISSING_PERMISSIONS);
            }
        } catch (ExpiredJwtException e) {
            log.warn("Expired token used by user: {}", e.getClaims().getSubject());
            throw new TokenValidationException("Token expired",
                    TokenValidationException.TokenErrorType.EXPIRED, e);
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
            throw new TokenValidationException("Token invalid",
                    TokenValidationException.TokenErrorType.INVALID_SIGNATURE, e);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            throw new TokenValidationException("Token validation failed",
                    TokenValidationException.TokenErrorType.GENERAL_ERROR, e);
        }
    }
    /**
     * Extract username from JWT token
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract username with full validation
     */
    public String extractUsernameWithValidation(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            log.warn("Expired token used by user: {}", e.getClaims().getSubject());
            throw new TokenValidationException("Token expired", 
                TokenValidationException.TokenErrorType.EXPIRED, e);
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
            throw new TokenValidationException("Token invalid", 
                TokenValidationException.TokenErrorType.INVALID_SIGNATURE, e);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            throw new TokenValidationException("Token validation failed", 
                TokenValidationException.TokenErrorType.GENERAL_ERROR, e);
        }
    }

    /**
     * Extract token version from JWT
     */
    public Integer extractTokenVersion(String token) {
        Claims claims = extractAllClaims(token);
        return (Integer) claims.get("tokenVersion");
    }

    /**
     * Extract permissions from JWT token
     */
    public Set<String> extractPermissions(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object codesObj = claims.get("permissionCodes");
            
            if (codesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> codesList = (List<String>) codesObj;
                return new HashSet<>(codesList);
            } else if (codesObj instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<String> codesSet = (Set<String>) codesObj;
                return codesSet;
            } else {
                log.warn("Invalid permission format in token for user: {}", claims.getSubject());
                throw new TokenValidationException("Invalid permission format in token", 
                    TokenValidationException.TokenErrorType.MISSING_PERMISSIONS);
            }
        } catch (ExpiredJwtException e) {
            log.warn("Expired token used by user: {}", e.getClaims().getSubject());
            throw new TokenValidationException("Token expired", 
                TokenValidationException.TokenErrorType.EXPIRED, e);
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
            throw new TokenValidationException("Token invalid", 
                TokenValidationException.TokenErrorType.INVALID_SIGNATURE, e);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            throw new TokenValidationException("Token validation failed", 
                TokenValidationException.TokenErrorType.GENERAL_ERROR, e);
        }
    }

    /**
     * Extract expiration date from JWT
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Check if token is expired
     */
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    public String extractJti(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("jti", String.class);
    }

    /**
     * Validate JWT token structure and signature
     */
    public Boolean validateTokenStructure(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token with token version check
     */
    public Boolean validateToken(String token, User user) {
        try {
            Claims claims = extractAllClaims(token);
            String username = claims.getSubject();
            Integer tokenVersion = (Integer) claims.get("tokenVersion");
            
            return username.equals(user.getUsername()) 
                && tokenVersion.equals(user.getTokenVersion())
                && !isTokenExpired(token);
        } catch (Exception e) {
            log.debug("Token validation failed for user {}: {}", user.getUsername(), e.getMessage());
            return false;
        }
    }

    /**
     * Extract all claims from JWT token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public void revokeToken(String jti, Date expiryDate) {
        revokedTokens.put(jti, expiryDate.getTime());
    }

    public boolean validateTokenWithJtiCheck(String token, User user) {
        try {
            Claims claims = extractAllClaims(token);
            String userIdFromToken = claims.getSubject(); // giờ subject là userId
            Integer tokenVersion = claims.get("tokenVersion", Integer.class);
            String jti = claims.get("jti", String.class);

            // Nếu token không chứa jti => không hợp lệ
            if (jti == null || jti.isBlank()) {
                log.warn("Token missing JTI for userId: {}", userIdFromToken);
                return false;
            }

            // Check JTI có tồn tại trong DB không
            boolean jtiExists = refreshTokenRepository.existsByJti(jti);
            if (!jtiExists) {
                log.warn("Token JTI {} not found in DB for userId: {}", jti, userIdFromToken);
                return false;
            }

            return userIdFromToken.equals(String.valueOf(user.getUserId())) // so sánh theo id
                    && tokenVersion.equals(user.getTokenVersion())
                    && !isTokenExpired(token);
        } catch (Exception e) {
            log.debug("Token validation with JTI failed for userId {}: {}",
                    user.getUserId(), e.getMessage());
            return false;
        }
    }


}
