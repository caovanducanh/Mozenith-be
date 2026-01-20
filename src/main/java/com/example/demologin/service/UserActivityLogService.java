package com.example.demologin.service;

import com.example.demologin.dto.request.userActivityLog.UserActivityLogExportRequest;
import com.example.demologin.dto.request.userActivityLog.UserActivityLogFilterRequest;
import com.example.demologin.dto.response.UserActivityLogResponse;
import com.example.demologin.entity.User;
import com.example.demologin.enums.ActivityType;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

public interface UserActivityLogService {
    
    void logUserActivity(User user, ActivityType activityType, String details);

    
    Page<UserActivityLogResponse> getAllActivityLogs(int page, int size);
    UserActivityLogResponse getActivityLogById(Long id);
    Page<UserActivityLogResponse> getActivityLogsByUserId(Long userId, int page, int size);
    Page<UserActivityLogResponse> getActivityLogsByType(String activityType, int page, int size);
    Page<UserActivityLogResponse> getActivityLogsByDateRange(LocalDateTime startTime, LocalDateTime endTime, int page, int size);
    Page<UserActivityLogResponse> exportActivityLogs(UserActivityLogExportRequest request, int page, int size);
    String deleteActivityLog(Long id);
    Page<UserActivityLogResponse> getMyLoginHistory(int page, int size);

}
