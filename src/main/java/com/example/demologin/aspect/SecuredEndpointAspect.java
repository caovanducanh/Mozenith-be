package com.example.demologin.aspect;

import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.exception.exceptions.*;
import com.example.demologin.service.RolePermissionService;
import com.example.demologin.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;

@Aspect
@Component
@Slf4j
public class SecuredEndpointAspect {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Around("@within(securedEndpoint) || @annotation(securedEndpoint)")
    public Object checkSecuredEndpoint(ProceedingJoinPoint joinPoint, SecuredEndpoint securedEndpoint) throws Throwable {
        // Resolve annotation from method or class
        securedEndpoint = resolveAnnotation(joinPoint, securedEndpoint);

        if (securedEndpoint == null) {
            log.warn("SecuredEndpoint annotation not found for method: {}", joinPoint.getSignature().getName());
            throw new AccessDenyException("Access denied");
        }

        String requiredPermission = securedEndpoint.value();
        log.debug("Checking permission: {} for method: {}", requiredPermission, joinPoint.getSignature().getName());

        // Check authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated access attempt to secured endpoint: {}", joinPoint.getSignature().getName());
            throw new UserNotAuthenticatedException("Authentication required");
        }

        // Extract token
        String token = extractTokenFromRequest();

        try {
            // Extract roles from token
            Set<String> userRoles = jwtUtil.extractRoles(token);
            String username = jwtUtil.extractUsernameWithValidation(token);

            // Get permissions
            Set<String> userPermissions = rolePermissionService.getPermissionsForRoles(userRoles);

            // Check permission
            if (!userPermissions.contains(requiredPermission)) {
                log.warn("Permission denied for user: {} - Required: {}, Available: {}",
                        username, requiredPermission, userPermissions);
                throw new AccessDenyException("Insufficient permissions");
            }

            log.debug("Permission check passed for user: {} with permission: {}", username, requiredPermission);
            return joinPoint.proceed();

        } catch (TokenValidationException e) {
            String errorMsg = handleTokenValidationException(e, joinPoint);
            throw new InvalidTokenException(errorMsg);
        }
    }

    private SecuredEndpoint resolveAnnotation(ProceedingJoinPoint joinPoint, SecuredEndpoint securedEndpoint) {
        if (securedEndpoint == null) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            securedEndpoint = method.getAnnotation(SecuredEndpoint.class);
            if (securedEndpoint == null) {
                securedEndpoint = method.getDeclaringClass().getAnnotation(SecuredEndpoint.class);
            }
        }
        return securedEndpoint;
    }

    private String extractTokenFromRequest() {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            throw new InvalidTokenException("Invalid authorization token format");
        }
        return token.substring(7);
    }

    private String handleTokenValidationException(TokenValidationException e, ProceedingJoinPoint joinPoint) {
        String endpoint = joinPoint.getSignature().getName();
        String errorMsg;

        switch (e.getErrorType()) {
            case EXPIRED:
                errorMsg = "Token has expired";
                log.warn("Token expired for endpoint: {} - {}", endpoint, e.getMessage());
                break;
            case INVALID_SIGNATURE:
                errorMsg = "Invalid token signature";
                log.warn("Invalid token signature for endpoint: {} - {}", endpoint, e.getMessage());
                break;
            case MISSING_PERMISSIONS:
                errorMsg = "Invalid token permissions format";
                log.warn("Missing permissions in token for endpoint: {} - {}", endpoint, e.getMessage());
                break;
            case MALFORMED:
                errorMsg = "Malformed token";
                log.warn("Malformed token for endpoint: {} - {}", endpoint, e.getMessage());
                break;
            default:
                errorMsg = "Token validation failed";
                log.warn("Token validation failed for endpoint: {} - {}", endpoint, e.getMessage());
                break;
        }

        return errorMsg;
    }
}