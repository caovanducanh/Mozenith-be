package com.example.demologin.dto.request.userActivityLog;

import com.example.demologin.enums.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityLogFilterRequest {
    
    private Long userId;
    private String username;
    private ActivityType activityType;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    // Pagination
    private int page = 0;
    private int size = 20;
    private String sortBy = "timestamp";
    private String sortDirection = "DESC";
}
