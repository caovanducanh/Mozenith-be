package com.example.demologin.service;

import com.example.demologin.dto.response.ResponseObject;
import com.example.demologin.entity.User;
import org.springframework.http.ResponseEntity;

/**
 * Service for managing token versions to invalidate user tokens
 * Token version is used to revoke all existing tokens when security requires it
 */
public interface TokenVersionService {
    
    /**
     * Business logic methods (for controllers)
     */
    ResponseEntity<ResponseObject> incrementCurrentUserTokenVersion();
    ResponseEntity<ResponseObject> incrementUserTokenVersionByUserId(Long userId);
    ResponseEntity<ResponseObject> getCurrentUserTokenVersion();
    ResponseEntity<ResponseObject> getUserTokenVersionByUserId(Long userId);
    
    /**
     * Raw service methods (for internal use)
     */
    User incrementTokenVersion(User user);
    User incrementTokenVersionByUserId(Long userId);
    User incrementTokenVersionByUsername(String username);
    
    /**
     * Get current token version for user
     * @param userId the user ID
     * @return current token version
     */
    int getCurrentTokenVersion(Long userId);
    
    /**
     * Get current token version for user by username
     * @param username the username
     * @return current token version
     */
    int getCurrentTokenVersionByUsername(String username);
    
    /**
     * Check if token version is valid for user
     * @param userId the user ID
     * @param tokenVersion the token version from JWT
     * @return true if token version is valid
     */
    boolean isTokenVersionValid(Long userId, int tokenVersion);
    
    /**
     * Check if token version is valid for user by username
     * @param username the username
     * @param tokenVersion the token version from JWT
     * @return true if token version is valid
     */
    boolean isTokenVersionValidByUsername(String username, int tokenVersion);
    
    /**
     * Invalidate all tokens for a user (useful for logout all devices)
     * @param userId the user ID
     */
    void invalidateAllTokens(Long userId);
    
    /**
     * Invalidate all tokens for a user by username
     * @param username the username
     */
    void invalidateAllTokensByUsername(String username);
    
    /**
     * Reset token version to 0 (useful for testing or admin operations)
     * @param userId the user ID
     * @return updated user
     */
    User resetTokenVersion(Long userId);
}
