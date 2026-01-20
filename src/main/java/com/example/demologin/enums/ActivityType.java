package com.example.demologin.enums;

public enum ActivityType {
    // Core authentication activities
    REGISTRATION,
    LOGIN_ATTEMPT,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    
    // Security activities  
    PASSWORD_CHANGE,
    TOKEN_REFRESH,
    OTP_VERIFICATION,
    EMAIL_VERIFICATION,
    
    // Profile activities
    PROFILE_UPDATE,
    PROFILE_VIEW,
    
    // Admin activities
    ADMIN_ACTION,
    
    // System activities
    SYSTEM_LOGIN,
    SYSTEM_LOGOUT,
    
    // Other
    OTHER
}
