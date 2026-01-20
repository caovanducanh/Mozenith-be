package com.example.demologin.aspect;

import com.example.demologin.annotation.UserActivity;
import com.example.demologin.entity.UserActivityLog;
import com.example.demologin.entity.User;
import com.example.demologin.exception.exceptions.InvalidPrincipalTypeException;
import com.example.demologin.exception.exceptions.UserActivityLoggingException;
import com.example.demologin.exception.exceptions.UserNotAuthenticatedException;
import com.example.demologin.repository.UserActivityLogRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.utils.*;
import com.example.demologin.dto.request.login.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class UserActivityAspect {

    private final UserActivityLogRepository userActivityLogRepository;
    private final UserRepository userRepository;
    private final AccountUtils accountUtils;
    private final IpUtilsWrapper ipUtils;
    private final UserAgentUtil userAgentUtil;
    private final LocationUtil locationUtil;

    @AfterReturning(value = "@annotation(userActivity)", returning = "result")
    public void logUserActivity(JoinPoint joinPoint, UserActivity userActivity, Object result) {
        try {
            User currentUser = getCurrentUserOrFromLoginAttempt(joinPoint, userActivity, result);
            UserActivityLog activityLog = createOrUpdateActivityLog(joinPoint, userActivity, currentUser);

            userActivityLogRepository.save(activityLog);
            logActivitySuccess(userActivity, activityLog);
        } catch (Exception e) {
            log.error("Failed to log user activity for method {}: {}",
                    joinPoint.getSignature().getName(), e.getMessage());
            saveFailedLogEntry(joinPoint, userActivity, e.getMessage());
        }
    }

    private User getCurrentUserOrFromLoginAttempt(JoinPoint joinPoint, UserActivity userActivity, Object result) {
        // For login attempts, try to get user from request first
        if ("LOGIN_ATTEMPT".equals(userActivity.activityType().name())) {
            try {
                return extractUserFromLoginRequest(joinPoint);
            } catch (UserActivityLoggingException e) {
                log.debug("Could not extract user from login request: {}", e.getMessage());
            }
        }

        // For other activities, get authenticated user
        try {
            return accountUtils.getCurrentUser();
        } catch (UserNotAuthenticatedException | InvalidPrincipalTypeException e) {
            return null;
        }
    }

    private User extractUserFromLoginRequest(JoinPoint joinPoint) throws UserActivityLoggingException {
        LoginRequest loginRequest = Arrays.stream(joinPoint.getArgs())
                .filter(arg -> arg instanceof LoginRequest)
                .map(arg -> (LoginRequest) arg)
                .findFirst()
                .orElseThrow(() -> new UserActivityLoggingException("No LoginRequest found in method arguments"));

        String username = loginRequest.getUsername();

        // Skip if username is invalid or anonymous
        if (username == null || username.equals("anonymousUser")) {
            throw new UserActivityLoggingException("Invalid username for login attempt");
        }

        if (EmailUtils.isValidEmail(username)) {
            return userRepository.findByEmail(username)
                    .orElseThrow(() -> new UserActivityLoggingException("User not found with email: " + username));
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserActivityLoggingException("User not found with username: " + username));
    }

    private UserActivityLog createOrUpdateActivityLog(JoinPoint joinPoint, UserActivity userActivity, User currentUser) {
        ClientInfo clientInfo = getClientInfo();
        UserAgentUtil.DeviceInfo deviceInfo = userAgentUtil.parseUserAgent(clientInfo.userAgent());
        LocationUtil.LocationInfo locationInfo = locationUtil.getLocationFromIP(clientInfo.ipForLocation());

        Long userId = userActivity.logUserId() && currentUser != null ? currentUser.getUserId() : null;
        String fullName = userActivity.logUserId() && currentUser != null ? currentUser.getFullName() : null;
        String status = "SUCCESS";
        String details = getDetails(userActivity, joinPoint);

        return findExistingActivityLog(userId, userActivity, clientInfo)
                .map(existingLog -> updateExistingLog(existingLog, fullName, details, deviceInfo, locationInfo))
                .orElseGet(() -> createNewActivityLog(userActivity, userId, fullName, status, details, clientInfo, deviceInfo, locationInfo));
    }

    private void saveFailedLogEntry(JoinPoint joinPoint, UserActivity userActivity, String error) {
        try {
            ClientInfo clientInfo = getClientInfo();

            UserActivityLog activityLog = UserActivityLog.builder()
                    .activityType(userActivity.activityType())
                    .status("FAILED")
                    .timestamp(LocalDateTime.now())
                    .details("Failed to log activity: " + error)
                    .ipAddress(clientInfo.clientIp())
                    .userAgent(clientInfo.userAgent())
                    .build();

            userActivityLogRepository.save(activityLog);
            log.debug("Saved failed activity log for {}", userActivity.activityType());
        } catch (Exception e) {
            log.error("Failed to save error log entry: {}", e.getMessage());
        }
    }

    private ClientInfo getClientInfo() {
        String rawIp = ipUtils.getClientIpAddress();
        String clientIp = formatIpAddress(rawIp);
        String userAgent = ipUtils.getUserAgent();
        String ipForLocation = getIpForLocation(clientIp);

        return new ClientInfo(clientIp, userAgent, ipForLocation);
    }

    private record ClientInfo(String clientIp, String userAgent, String ipForLocation) {}

    private String formatIpAddress(String rawIp) {
        if (rawIp == null) return "unknown";
        if (rawIp.equals("127.0.0.1") || rawIp.equals("::1")) {
            return rawIp + " (localhost)";
        }
        return rawIp;
    }

    private String getIpForLocation(String clientIp) {
        if (clientIp.contains("(localhost)")) {
            return clientIp.substring(0, clientIp.indexOf(" (localhost)"));
        }
        return clientIp.equals("127.0.0.1") || clientIp.equals("::1") ? "8.8.8.8" : clientIp;
    }

    private String getDetails(UserActivity userActivity, JoinPoint joinPoint) {
        return userActivity.details().isEmpty()
                ? String.format("%s - %s", userActivity.activityType(), joinPoint.getSignature().getName())
                : userActivity.details();
    }

    private Optional<UserActivityLog> findExistingActivityLog(Long userId, UserActivity userActivity, ClientInfo clientInfo) {
        if (userId == null) return Optional.empty();

        return Optional.ofNullable(
                userActivityLogRepository.findTopByUserIdAndActivityTypeAndIpAddressAndUserAgentOrderByTimestampDesc(
                        userId, userActivity.activityType(), clientInfo.clientIp(), clientInfo.userAgent())
        );
    }

    private UserActivityLog updateExistingLog(UserActivityLog existingLog, String fullName, String details,
                                              UserAgentUtil.DeviceInfo deviceInfo, LocationUtil.LocationInfo locationInfo) {
        existingLog.setTimestamp(LocalDateTime.now());
        existingLog.setFullName(fullName);
        existingLog.setDetails(details);
        updateDeviceInfo(existingLog, deviceInfo);
        updateLocationInfo(existingLog, locationInfo);

        log.debug("Updated existing user activity log for user {} from {}", fullName, existingLog.getIpAddress());
        return existingLog;
    }

    private UserActivityLog createNewActivityLog(UserActivity userActivity, Long userId, String fullName, String status,
                                                 String details, ClientInfo clientInfo, UserAgentUtil.DeviceInfo deviceInfo,
                                                 LocationUtil.LocationInfo locationInfo) {
        UserActivityLog activityLog = UserActivityLog.builder()
                .activityType(userActivity.activityType())
                .userId(userId)
                .fullName(fullName)
                .status(status)
                .timestamp(LocalDateTime.now())
                .details(details)
                .ipAddress(clientInfo.clientIp())
                .userAgent(clientInfo.userAgent())
                .browser(deviceInfo.getBrowser())
                .browserVersion(deviceInfo.getBrowserVersion())
                .operatingSystem(deviceInfo.getOperatingSystem())
                .device(deviceInfo.getDevice())
                .deviceType(deviceInfo.getDeviceType())
                .city(locationInfo.getCity())
                .region(locationInfo.getRegion())
                .country(locationInfo.getCountry())
                .countryCode(locationInfo.getCountryCode())
                .build();

        log.debug("Created new user activity log for user {} from {}", fullName, clientInfo.clientIp());
        return activityLog;
    }

    private void updateDeviceInfo(UserActivityLog log, UserAgentUtil.DeviceInfo deviceInfo) {
        log.setBrowser(deviceInfo.getBrowser());
        log.setBrowserVersion(deviceInfo.getBrowserVersion());
        log.setOperatingSystem(deviceInfo.getOperatingSystem());
        log.setDevice(deviceInfo.getDevice());
        log.setDeviceType(deviceInfo.getDeviceType());
    }

    private void updateLocationInfo(UserActivityLog log, LocationUtil.LocationInfo locationInfo) {
        log.setCity(locationInfo.getCity());
        log.setRegion(locationInfo.getRegion());
        log.setCountry(locationInfo.getCountry());
        log.setCountryCode(locationInfo.getCountryCode());
    }

    private void logActivitySuccess(UserActivity userActivity, UserActivityLog activityLog) {
        log.debug("User activity logged: {} for user {} from {} using {}",
                userActivity.activityType(),
                activityLog.getFullName() != null ? activityLog.getFullName() : "anonymous",
                LocationUtil.formatLocationInfo(locationUtil.getLocationFromIP(activityLog.getIpAddress())),
                UserAgentUtil.formatDeviceInfo(userAgentUtil.parseUserAgent(activityLog.getUserAgent())));
    }
}