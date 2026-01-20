package com.example.demologin.serviceImpl;

import com.example.demologin.dto.response.ResponseObject;
import com.example.demologin.entity.User;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.TokenVersionService;
import com.example.demologin.utils.AccountUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TokenVersionServiceImpl implements TokenVersionService {

    private final UserRepository userRepository;
    private final AccountUtils accountUtils;

    @Override
    public User incrementTokenVersion(User user) {
        log.info("Incrementing token version for user: {}", user.getUsername());
        user.incrementTokenVersion();
        User savedUser = userRepository.save(user);
        log.info("Token version incremented to {} for user: {}", savedUser.getTokenVersion(), user.getUsername());
        return savedUser;
    }

    @Override
    public User incrementTokenVersionByUserId(Long userId) {
        log.info("Incrementing token version for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        return incrementTokenVersion(user);
    }

    @Override
    public User incrementTokenVersionByUsername(String username) {
        log.info("Incrementing token version for username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found with username: " + username));
        return incrementTokenVersion(user);
    }

    @Override
    @Transactional(readOnly = true)
    public int getCurrentTokenVersion(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        return user.getTokenVersion();
    }

    @Override
    @Transactional(readOnly = true)
    public int getCurrentTokenVersionByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found with username: " + username));
        return user.getTokenVersion();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTokenVersionValid(Long userId, int tokenVersion) {
        int currentVersion = getCurrentTokenVersion(userId);
        boolean isValid = currentVersion == tokenVersion;
        log.debug("Token version validation for user {}: current={}, provided={}, valid={}",
                userId, currentVersion, tokenVersion, isValid);
        return isValid;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTokenVersionValidByUsername(String username, int tokenVersion) {
        int currentVersion = getCurrentTokenVersionByUsername(username);
        boolean isValid = currentVersion == tokenVersion;
        log.debug("Token version validation for username {}: current={}, provided={}, valid={}",
                username, currentVersion, tokenVersion, isValid);
        return isValid;
    }

    @Override
    public void invalidateAllTokens(Long userId) {
        log.info("Invalidating all tokens for user ID: {}", userId);
        incrementTokenVersionByUserId(userId);
        log.info("All tokens invalidated for user ID: {}", userId);
    }

    @Override
    public void invalidateAllTokensByUsername(String username) {
        log.info("Invalidating all tokens for username: {}", username);
        incrementTokenVersionByUsername(username);
        log.info("All tokens invalidated for username: {}", username);
    }

    @Override
    public User resetTokenVersion(Long userId) {
        log.info("Resetting token version for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        user.setTokenVersion(0);
        User savedUser = userRepository.save(user);
        log.info("Token version reset to 0 for user: {}", user.getUsername());
        return savedUser;
    }

    // Business logic methods for controllers
    @Override
    public ResponseEntity<ResponseObject> incrementCurrentUserTokenVersion() {
        User currentUser = accountUtils.getCurrentUser();
        User updatedUser = incrementTokenVersion(currentUser);

        Map<String, Object> data = Map.of(
                "username", updatedUser.getUsername(),
                "oldTokenVersion", currentUser.getTokenVersion(),
                "newTokenVersion", updatedUser.getTokenVersion(),
                "updatedAt", LocalDateTime.now(),
                "message", "All existing tokens have been invalidated"
        );

        return ResponseEntity.ok(new ResponseObject(HttpStatus.OK.value(), "Token version incremented successfully", data));
    }

    @Override
    public ResponseEntity<ResponseObject> incrementUserTokenVersionByUserId(Long userId) {
        User adminUser = accountUtils.getCurrentUser();
        User updatedUser = incrementTokenVersionByUserId(userId);

        Map<String, Object> data = Map.of(
                "targetUserId", userId,
                "targetUsername", updatedUser.getUsername(),
                "newTokenVersion", updatedUser.getTokenVersion(),
                "updatedBy", adminUser.getUsername(),
                "updatedAt", LocalDateTime.now(),
                "message", "All existing tokens for target user have been invalidated"
        );

        return ResponseEntity.ok(new ResponseObject(HttpStatus.OK.value(), "User token version incremented successfully", data));
    }

    @Override
    public ResponseEntity<ResponseObject> getCurrentUserTokenVersion() {
        User currentUser = accountUtils.getCurrentUser();

        Map<String, Object> data = Map.of(
                "username", currentUser.getUsername(),
                "tokenVersion", currentUser.getTokenVersion(),
                "checkedAt", LocalDateTime.now()
        );

        return ResponseEntity.ok(new ResponseObject(HttpStatus.OK.value(), "Token version retrieved successfully", data));
    }

    @Override
    public ResponseEntity<ResponseObject> getUserTokenVersionByUserId(Long userId) {
        int tokenVersion = getCurrentTokenVersion(userId);
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        Map<String, Object> data = Map.of(
                "userId", userId,
                "username", targetUser.getUsername(),
                "tokenVersion", tokenVersion,
                "checkedAt", LocalDateTime.now()
        );

        return ResponseEntity.ok(new ResponseObject(HttpStatus.OK.value(), "User token version retrieved successfully", data));
    }
}
