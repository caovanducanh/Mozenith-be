package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.annotation.UserActivity;
import com.example.demologin.enums.ActivityType;
import com.example.demologin.service.SessionManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Session Management", description = "APIs for managing user sessions and device logout")
    public class SessionManagementController {
    
    private final SessionManagementService sessionManagementService;
    
    @UserActivity(activityType = ActivityType.LOGOUT, details = "User logout from current device")
    @PostMapping("/logout")
    @SecuredEndpoint("USER_TOKEN_MANAGEMENT")
    @Operation(summary = "Logout from current device", 
               description = "Logout user from current device only")
    @ApiResponse(message = "Logged out from current device successfully")
    public Object logoutCurrentDevice()
    {
        sessionManagementService.logoutCurrentDevice();
        return null;
    }
    
    @UserActivity(activityType = ActivityType.LOGOUT, details = "User logout from all devices")
    @PostMapping("/logout-all")
    @SecuredEndpoint("USER_TOKEN_MANAGEMENT")
    @Operation(summary = "Logout from all devices", 
               description = "Logout user from all devices by invalidating all tokens")
    @ApiResponse(message = "Logged out from all devices successfully")
    public Object logoutFromAllDevices()
    {
        sessionManagementService.logoutFromAllDevices();
        return null;
    }
    
    @PostMapping("/force-logout/{userId}")
    @SecuredEndpoint("ADMIN_USER_MANAGEMENT")
    @Operation(summary = "Admin force logout user from all devices", 
               description = "Admin operation to force logout a specific user from all devices")
    @ApiResponse(message = "User forced logout successfully")
    public Object forceLogoutUser(
            @Parameter(description = "User ID to force logout") @PathVariable Long userId)
    {
        sessionManagementService.forceLogoutUser(userId);
        return null;
    }
    

}
