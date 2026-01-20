package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.userActivityLog.UserActivityLogExportRequest;
import com.example.demologin.service.UserActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/user-activity-logs")
@RequiredArgsConstructor
@Tag(name = "User Activity Log Management", description = "APIs for managing user activity logs (login, logout, registration, etc.)")
    public class UserActivityLogController {

    private final UserActivityLogService userActivityLogService;

    @GetMapping
    @PageResponse
    @ApiResponse(message = "Activity logs retrieved successfully")
    @SecuredEndpoint("LOG_VIEW_ACTIVITY")
    @Operation(summary = "Get all user activity logs", description = "Retrieve paginated list of all user activity logs")
    public Object getAllActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return userActivityLogService.getAllActivityLogs(page, size);
    }

    @GetMapping("/{id}")
    @ApiResponse(message = "Activity log retrieved successfully")
    @SecuredEndpoint("LOG_VIEW_ACTIVITY")
    @Operation(summary = "Get activity log by ID", description = "Retrieve a specific activity log by its ID")
    public Object getActivityLogById(
            @Parameter(description = "Activity log ID") @PathVariable Long id) {
        
        return userActivityLogService.getActivityLogById(id);
    }

    @GetMapping("/user/{userId}")
    @PageResponse
    @ApiResponse(message = "User activity logs retrieved successfully")
    @SecuredEndpoint("LOG_VIEW_ACTIVITY")
    @Operation(summary = "Get activity logs by user ID", description = "Retrieve paginated activity logs for a specific user")
    public Object getActivityLogsByUserId(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return userActivityLogService.getActivityLogsByUserId(userId, page, size);
    }

    @GetMapping("/type/{actionType}")
    @PageResponse
    @ApiResponse(message = "Activity logs by type retrieved successfully")
    @SecuredEndpoint("LOG_VIEW_ACTIVITY")
    @Operation(summary = "Get activity logs by action type", description = "Retrieve paginated activity logs filtered by action type")
    public Object getActivityLogsByActionType(
            @Parameter(description = "Action type") @PathVariable String actionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return userActivityLogService.getActivityLogsByType(actionType, page, size);
    }

    @GetMapping("/date-range")
    @PageResponse
    @ApiResponse(message = "Activity logs by date range retrieved successfully")
    @SecuredEndpoint("LOG_VIEW_ACTIVITY")
    @Operation(summary = "Get activity logs by date range", description = "Retrieve paginated activity logs within a date range")
    public Object getActivityLogsByDateRange(
            @Parameter(description = "Start date (YYYY-MM-DD)") @RequestParam String startDate,
            @Parameter(description = "End date (YYYY-MM-DD)") @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(23, 59, 59);
        return userActivityLogService.getActivityLogsByDateRange(startDateTime, endDateTime, page, size);
    }

    @PostMapping("/export")
    @PageResponse
    @ApiResponse(message = "Activity logs exported successfully")
    @SecuredEndpoint("ADMIN_ACTIVITY_LOG_EXPORT")
    @Operation(summary = "Export activity logs", description = "Export activity logs within date range with pagination")
    public Object exportActivityLogs(
            @Valid @RequestBody UserActivityLogExportRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return userActivityLogService.exportActivityLogs(request, page, size);
    }

    @GetMapping("/my-login-history")
    @PageResponse
    @ApiResponse(message = "Login history retrieved successfully")
    @SecuredEndpoint("USER_VIEW_OWN_LOGIN_HISTORY")
    @Operation(summary = "Get my login history", description = "Retrieve paginated login history for the current authenticated user")
    public Object getMyLoginHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return userActivityLogService.getMyLoginHistory(page, size);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(message = "Activity log deleted successfully")
    @SecuredEndpoint("LOG_DELETE")
    @Operation(summary = "Delete activity log", description = "Delete a specific activity log by its ID")
    public Object deleteActivityLog(
            @Parameter(description = "Activity log ID") @PathVariable Long id) {
        
        return userActivityLogService.deleteActivityLog(id);
    }
}
