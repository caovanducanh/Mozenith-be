package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.userActivityLog.UserActivityLogExportRequest;
import com.example.demologin.dto.response.UserActivityLogResponse;
import com.example.demologin.entity.UserActivityLog;
import com.example.demologin.entity.User;
import com.example.demologin.enums.ActivityType;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.mapper.UserActivityLogMapper;
import com.example.demologin.repository.UserActivityLogRepository;
import com.example.demologin.service.UserActivityLogService;
import com.example.demologin.utils.AccountUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityLogServiceImpl implements UserActivityLogService {

    private final UserActivityLogRepository userActivityLogRepository;
    private final UserActivityLogMapper userActivityLogMapper;
    private final AccountUtils accountUtils;

    @Override
    @Transactional
    public void logUserActivity(User user, ActivityType activityType, String details) {
        try {
            UserActivityLog activityLog = UserActivityLog.builder()
                .userId(user.getUserId())
                .activityType(activityType)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();

            userActivityLogRepository.save(activityLog);
            log.info("Logged activity for user {}: {} - {}", 
                user.getUsername(), activityType, details);
        } catch (Exception e) {
            log.error("Failed to log user activity: {}", e.getMessage());
        }
    }

    @Override
    public Page<UserActivityLogResponse> getAllActivityLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserActivityLog> logs = userActivityLogRepository.findAll(pageable);
        Page<UserActivityLogResponse> mappedLogs = logs.map(userActivityLogMapper::toResponse);
        
        if (mappedLogs.getContent().isEmpty()) {
            throw new NotFoundException("No activity logs found");
        }
        return mappedLogs;
    }

    @Override
    public UserActivityLogResponse getActivityLogById(Long id) {
        UserActivityLog log = userActivityLogRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Activity log not found with ID: " + id));
        return userActivityLogMapper.toResponse(log);
    }

    @Override
    public Page<UserActivityLogResponse> getActivityLogsByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserActivityLog> logs = userActivityLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
        Page<UserActivityLogResponse> mappedLogs = logs.map(userActivityLogMapper::toResponse);
        
        if (mappedLogs.getContent().isEmpty()) {
            throw new NotFoundException("No activity logs found for user ID: " + userId);
        }
        return mappedLogs;
    }

    @Override
    public Page<UserActivityLogResponse> getActivityLogsByType(String activityType, int page, int size) {
        try {
            ActivityType type = ActivityType.valueOf(activityType.toUpperCase());
            Pageable pageable = PageRequest.of(page, size);
            Page<UserActivityLog> logs = userActivityLogRepository.findByActivityTypeOrderByTimestampDesc(type, pageable);
            Page<UserActivityLogResponse> mappedLogs = logs.map(userActivityLogMapper::toResponse);
            
            if (mappedLogs.getContent().isEmpty()) {
                throw new NotFoundException("No activity logs found for activity type: " + activityType);
            }
            return mappedLogs;
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Invalid activity type: " + activityType);
        }
    }

    @Override
    public Page<UserActivityLogResponse> getActivityLogsByDateRange(LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserActivityLog> logs = userActivityLogRepository.findByTimestampBetween(startTime, endTime, pageable);
        Page<UserActivityLogResponse> mappedLogs = logs.map(userActivityLogMapper::toResponse);
        
        if (mappedLogs.getContent().isEmpty()) {
            throw new NotFoundException("No activity logs found for the specified date range");
        }
        return mappedLogs;
    }

    @Override
    public Page<UserActivityLogResponse> exportActivityLogs(UserActivityLogExportRequest request, int page, int size) {
        // Build dynamic query based on request filters
        Pageable pageable = PageRequest.of(page, size);
        
        // For now, export all - you can add filtering logic based on request
        Page<UserActivityLog> logs = userActivityLogRepository.findAll(pageable);
        Page<UserActivityLogResponse> mappedLogs = logs.map(userActivityLogMapper::toResponse);
        
        if (mappedLogs.getContent().isEmpty()) {
            throw new NotFoundException("No activity logs found for export");
        }
        return mappedLogs;
    }

    @Override
    public String deleteActivityLog(Long id) {
        UserActivityLog log = userActivityLogRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Activity log not found with ID: " + id));
        
        userActivityLogRepository.delete(log);
        return "Activity log deleted successfully";
    }

    @Override
    public Page<UserActivityLogResponse> getMyLoginHistory(int page, int size) {
        User currentUser = accountUtils.getCurrentUser();
        
        if (currentUser == null) {
            throw new NotFoundException("Current user not found");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserActivityLog> loginLogs = userActivityLogRepository
            .findByUserIdAndActivityTypeOrderByTimestampDesc(
                currentUser.getUserId(), 
                ActivityType.LOGIN_ATTEMPT,
                pageable
            );

        if (loginLogs.getContent().isEmpty()) {
            throw new NotFoundException("No login history found for current user");
        }
        
        return loginLogs.map(userActivityLogMapper::toResponse);
    }
}
