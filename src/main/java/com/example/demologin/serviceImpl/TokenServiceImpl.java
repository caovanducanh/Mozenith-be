package com.example.demologin.serviceImpl;

import com.example.demologin.entity.User;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.exception.exceptions.ValidationException;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.TokenService;
import com.example.demologin.utils.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of TokenService that handles JWT token business logic
 * while delegating JWT operations to JwtUtil.
 */
@Service
@Slf4j
@AllArgsConstructor
public class TokenServiceImpl implements TokenService {
    

    private final JwtUtil jwtUtil;
    

    private final UserRepository userRepository;

    @Override
    public String generateTokenForUser(User user) {
        log.debug("Generating token for user: {}", user.getUsername());
        return jwtUtil.generateToken(user);
    }

    public User getUserByToken(String token) {
        try {
            String userIdStr = jwtUtil.extractUsername(token); // vẫn dùng method cũ, nhưng thực chất đang lấy subject
            if (userIdStr == null || userIdStr.trim().isEmpty()) {
                throw new ValidationException("Invalid token: userId not found");
            }

            Long userId = Long.parseLong(userIdStr);

            return userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
        } catch (Exception e) {
            log.warn("Failed to extract user from token: {}", e.getMessage());
            throw new ValidationException("Invalid token: " + e.getMessage());
        }
    }

}
