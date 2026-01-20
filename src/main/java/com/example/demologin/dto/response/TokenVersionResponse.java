package com.example.demologin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenVersionResponse {
    private Long userId;
    private String username;
    private int tokenVersion;
    private String message;
    private LocalDateTime timestamp;
    
    public static TokenVersionResponse success(Long userId, String username, int tokenVersion, String message) {
        return TokenVersionResponse.builder()
                .userId(userId)
                .username(username)
                .tokenVersion(tokenVersion)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static TokenVersionResponse invalidated(Long userId, String username, int newTokenVersion) {
        return success(userId, username, newTokenVersion, "All tokens have been invalidated successfully");
    }
    
    public static TokenVersionResponse reset(Long userId, String username) {
        return success(userId, username, 0, "Token version has been reset to 0");
    }
}
