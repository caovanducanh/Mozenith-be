package com.example.demologin.exception.exceptions;

/**
 * Exception thrown when JWT token validation fails.
 * 
 * This provides more specific error handling for different token validation scenarios:
 * - Expired tokens
 * - Invalid signatures
 * - Malformed tokens
 * - Missing permissions
 */
public class TokenValidationException extends RuntimeException {
    
    private final TokenErrorType errorType;
    
    public enum TokenErrorType {
        EXPIRED,
        INVALID_SIGNATURE,
        MALFORMED,
        MISSING_PERMISSIONS,
        GENERAL_ERROR
    }
    
    public TokenValidationException(String message, TokenErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }
    
    public TokenValidationException(String message, TokenErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }
    
    public TokenErrorType getErrorType() {
        return errorType;
    }
}
