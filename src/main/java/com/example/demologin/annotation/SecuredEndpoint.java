package com.example.demologin.annotation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.lang.annotation.*;

/**
 * Composite annotation that combines security annotations for API endpoints.
 * 
 * This annotation replaces the old pattern of multiple security annotations
 * and provides a unified security approach.
 * 
 * Usage:
 * @SecuredEndpoint("TOKEN_INVALIDATE_OWN")
 * public ResponseEntity<?> someMethod() { ... }
 * 
 * Benefits:
 * - Reduces code duplication
 * - Ensures consistent security setup
 * - Single point to modify security pattern
 * - Automatic Swagger documentation
 * - Supports both method-level and class-level usage
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SecurityRequirement(name = "bearerAuth")
public @interface SecuredEndpoint {
    /**
     * The permission code required to access this endpoint.
     * This will be validated against the user's JWT token permissions.
     */
    String value();
}
