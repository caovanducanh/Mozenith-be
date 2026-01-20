package com.example.demologin.repository;

import com.example.demologin.entity.UserActivityLog;
import com.example.demologin.enums.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {
    
    // Find by user ID
    Page<UserActivityLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
    
    // Find by activity type
    Page<UserActivityLog> findByActivityTypeOrderByTimestampDesc(ActivityType activityType, Pageable pageable);
    
    // Find by status
    Page<UserActivityLog> findByStatusOrderByTimestampDesc(String status, Pageable pageable);
    
    // Find by date range
    @Query("SELECT u FROM UserActivityLog u WHERE u.timestamp BETWEEN :startTime AND :endTime ORDER BY u.timestamp DESC")
    Page<UserActivityLog> findByTimestampBetween(@Param("startTime") LocalDateTime startTime, 
                                                @Param("endTime") LocalDateTime endTime, 
                                                Pageable pageable);
    
    @Query("SELECT u FROM UserActivityLog u WHERE u.timestamp BETWEEN :startTime AND :endTime ORDER BY u.timestamp DESC")
    List<UserActivityLog> findByTimestampBetween(@Param("startTime") LocalDateTime startTime, 
                                                @Param("endTime") LocalDateTime endTime);
    
    // Complex filter query
    @Query("SELECT u FROM UserActivityLog u WHERE " +
           "(:userId IS NULL OR u.userId = :userId) AND " +
           "(:activityType IS NULL OR u.activityType = :activityType) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:startTime IS NULL OR u.timestamp >= :startTime) AND " +
           "(:endTime IS NULL OR u.timestamp <= :endTime) " +
           "ORDER BY u.timestamp DESC")
    Page<UserActivityLog> findWithFilters(@Param("userId") Long userId,
                                         @Param("activityType") ActivityType activityType,
                                         @Param("status") String status,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime,
                                         Pageable pageable);
    
    // Get activity stats
    @Query("SELECT u.activityType, COUNT(u) FROM UserActivityLog u WHERE u.timestamp >= :startTime GROUP BY u.activityType")
    List<Object[]> getActivityStatsSince(@Param("startTime") LocalDateTime startTime);
    
    // Get user activity count
    @Query("SELECT COUNT(u) FROM UserActivityLog u WHERE u.userId = :userId AND u.timestamp >= :startTime")
    Long countUserActivitySince(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);
    
    // Find existing log to update instead of creating duplicate
    UserActivityLog findTopByUserIdAndActivityTypeAndIpAddressAndUserAgentOrderByTimestampDesc(
        Long userId, ActivityType activityType, String ipAddress, String userAgent);
    
    // Find login history for a specific user
    Page<UserActivityLog> findByUserIdAndActivityTypeOrderByTimestampDesc(
        Long userId, ActivityType activityType, Pageable pageable);

    void deleteByUserId(Long userId);
}

