package com.example.demologin.service;

import com.example.demologin.entity.User;

/**
 * Service for token-related business operations
 * For JWT utilities, use JwtUtil class instead
 */
public interface TokenService {
    
    /**
     * Generate token for user with business logic validation
     */
    String generateTokenForUser(User user);
    
    /**
     * Get user account by token with full validation
     */
    User getUserByToken(String token);
}
