package com.example.demologin.serviceImpl;

import com.example.demologin.entity.User;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.RefreshTokenRepository;
import com.example.demologin.repository.UserActivityLogRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.SessionManagementService;
import com.example.demologin.service.TokenVersionService;
import com.example.demologin.utils.AccountUtils;
import com.example.demologin.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementServiceImpl implements SessionManagementService {
    
    private final TokenVersionService tokenVersionService;
    private final UserRepository userRepository;
    private final AccountUtils accountUtils;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserActivityLogRepository userActivityLogReppsitory;

    @Override
    @Transactional
    public void logoutCurrentDevice() {
        String currentToken = accountUtils.getCurrentToken();
        String jti = jwtUtil.extractJti(currentToken);
        Date expiryDate = jwtUtil.extractExpiration(currentToken);

        // Xóa refresh token khỏi DB
        refreshTokenRepository.deleteByJti(jti);

        // Revoke access token trong memory
        jwtUtil.revokeToken(jti, expiryDate);

        log.info("User {} logged out from current device", accountUtils.getCurrentUser().getUsername());
    }


    @Override
    @Transactional
    public void logoutFromAllDevices() {
        User currentUser = accountUtils.getCurrentUser();
        log.info("User {} logging out from all devices", currentUser.getUsername());
        refreshTokenRepository.deleteByUser(currentUser);
        // Invalidate all tokens by incrementing token version
        tokenVersionService.incrementTokenVersionByUserId(currentUser.getUserId());
        userActivityLogReppsitory.deleteByUserId(currentUser.getUserId());
    }
    
    @Override
    @Transactional
    public void forceLogoutUser(Long userId) {
        User targetUser = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        
        User adminUser = accountUtils.getCurrentUser();
        log.info("Admin {} force logging out user {} from all devices", 
                adminUser.getUsername(), targetUser.getUsername());
        
        // Invalidate all tokens for target user
        tokenVersionService.incrementTokenVersionByUserId(userId);
    }
}
