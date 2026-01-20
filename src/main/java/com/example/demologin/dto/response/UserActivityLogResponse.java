package com.example.demologin.dto.response;

import com.example.demologin.enums.ActivityType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityLogResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("activityType")
    private ActivityType activityType;

    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("status")
    private String status;

    @JsonProperty("details")
    private String details;
    
    @JsonProperty("ipAddress")
    private String ipAddress;
    
    @JsonProperty("userAgent")
    private String userAgent;

    // Device and browser information
    @JsonProperty("browser")
    private String browser;

    @JsonProperty("browserVersion")
    private String browserVersion;

    @JsonProperty("operatingSystem")
    private String operatingSystem;

    @JsonProperty("device")
    private String device;

    @JsonProperty("deviceType")
    private String deviceType;

    // Location information
    @JsonProperty("city")
    private String city;

    @JsonProperty("region")
    private String region;

    @JsonProperty("country")
    private String country;

    @JsonProperty("countryCode")
    private String countryCode;

    // Formatted information for display
    @JsonProperty("deviceInfo")
    private String deviceInfo;

    @JsonProperty("location")
    private String location;
}
