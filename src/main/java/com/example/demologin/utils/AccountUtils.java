package com.example.demologin.utils;

import com.example.demologin.entity.User;
import com.example.demologin.exception.exceptions.InvalidPrincipalTypeException;
import com.example.demologin.exception.exceptions.UserNotAuthenticatedException;
import com.example.demologin.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@AllArgsConstructor
public class AccountUtils {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserNotAuthenticatedException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return user;
        }

        if (principal instanceof String username) {
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        }

        throw new InvalidPrincipalTypeException("Principal is of unsupported type: " + principal.getClass().getName());
    }

    public String getCurrentToken() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new UserNotAuthenticatedException("No request context found");
        }

        HttpServletRequest request = attrs.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // bỏ "Bearer "
        }

        throw new UserNotAuthenticatedException("No Bearer token found in request");
    }
}
