package com.example.demologin.mapper;

import com.example.demologin.dto.response.UserActivityLogResponse;
import com.example.demologin.entity.UserActivityLog;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.utils.LocationUtil;
import com.example.demologin.utils.UserAgentUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserActivityLogMapper {

    private final UserRepository userRepository;

    public UserActivityLogResponse toResponse(UserActivityLog log) {
        UserActivityLogResponse response = new UserActivityLogResponse();
        response.setId(log.getId());
        response.setActivityType(log.getActivityType());
        response.setUserId(log.getUserId());
        response.setTimestamp(log.getTimestamp());
        response.setStatus(log.getStatus());
        response.setDetails(log.getDetails());
        response.setIpAddress(log.getIpAddress());
        response.setUserAgent(log.getUserAgent());
        
        // Set user information if userId exists
        if (log.getUserId() != null) {
            userRepository.findById(log.getUserId())
                .ifPresent(user -> response.setFullName(user.getFullName()));
        } else {
            response.setFullName(log.getFullName());
        }
        
        // Set device and browser information
        response.setBrowser(log.getBrowser());
        response.setBrowserVersion(log.getBrowserVersion());
        response.setOperatingSystem(log.getOperatingSystem());
        response.setDevice(log.getDevice());
        response.setDeviceType(log.getDeviceType());
        
        // Set location information
        response.setCity(log.getCity());
        response.setRegion(log.getRegion());
        response.setCountry(log.getCountry());
        response.setCountryCode(log.getCountryCode());
        
        // Format device info for display
        if (log.getBrowser() != null && log.getOperatingSystem() != null) {
            UserAgentUtil.DeviceInfo deviceInfo = new UserAgentUtil.DeviceInfo(
                log.getBrowser(), 
                log.getBrowserVersion(), 
                log.getOperatingSystem(), 
                log.getDevice(), 
                log.getDeviceType()
            );
            response.setDeviceInfo(UserAgentUtil.formatDeviceInfo(deviceInfo));
        }
        
        // Format location for display
        if (log.getCity() != null && log.getCountry() != null) {
            LocationUtil.LocationInfo locationInfo = new LocationUtil.LocationInfo(
                log.getCity(), 
                log.getRegion(), 
                log.getCountry(), 
                log.getCountryCode()
            );
            response.setLocation(LocationUtil.formatLocationInfo(locationInfo));
        }
        
        return response;
    }

    public List<UserActivityLogResponse> toResponseList(List<UserActivityLog> logs) {
        return logs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
