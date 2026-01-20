package com.example.demologin.service;

/**
 * Service for handling user session management and device logout
 */
public interface SessionManagementService {
    
    /**
     * Business logic methods (for controllers)
     */
    void logoutCurrentDevice();
    void logoutFromAllDevices();
    void forceLogoutUser(Long userId);
}
