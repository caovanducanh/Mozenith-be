package com.example.demologin.aspect;

import com.example.demologin.annotation.UserActivity;
import com.example.demologin.entity.User;
import com.example.demologin.entity.UserActivityLog;
import com.example.demologin.repository.UserActivityLogRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.utils.AccountUtils;
import com.example.demologin.utils.IpUtilsWrapper;
import com.example.demologin.utils.LocationUtil;
import com.example.demologin.utils.UserAgentUtil;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class UserActivityAspectTest {
    @Test
    void testExtractUserFromLoginRequest_nullUsername() throws Exception {
        com.example.demologin.dto.request.login.LoginRequest loginRequest = mock(com.example.demologin.dto.request.login.LoginRequest.class);
        when(loginRequest.getUsername()).thenReturn(null);
        when(joinPoint.getArgs()).thenReturn(new Object[]{loginRequest});
        var method = aspect.getClass().getDeclaredMethod("extractUserFromLoginRequest", JoinPoint.class);
        method.setAccessible(true);
        assertThrows(com.example.demologin.exception.exceptions.UserActivityLoggingException.class, () -> {
            try {
                method.invoke(aspect, joinPoint);
            } catch (Exception e) {
                throw e.getCause();
            }
        });
    }

    @Test
    void testExtractUserFromLoginRequest_validEmailNotFound() throws Exception {
        com.example.demologin.dto.request.login.LoginRequest loginRequest = mock(com.example.demologin.dto.request.login.LoginRequest.class);
        when(loginRequest.getUsername()).thenReturn("test@email.com");
        when(joinPoint.getArgs()).thenReturn(new Object[]{loginRequest});
        try (var emailUtilsMocked = org.mockito.Mockito.mockStatic(com.example.demologin.utils.EmailUtils.class)) {
            emailUtilsMocked.when(() -> com.example.demologin.utils.EmailUtils.isValidEmail(any())).thenReturn(true);
            when(userRepository.findByEmail(eq("test@email.com"))).thenReturn(java.util.Optional.empty());
            var method = aspect.getClass().getDeclaredMethod("extractUserFromLoginRequest", JoinPoint.class);
            method.setAccessible(true);
            assertThrows(com.example.demologin.exception.exceptions.UserActivityLoggingException.class, () -> {
                try {
                    method.invoke(aspect, joinPoint);
                } catch (Exception e) {
                    throw e.getCause();
                }
            });
        }
    }
    @Test
    void testSaveFailedLogEntry_catchInCatch() throws Exception {
        // Ném exception trong save để vào nhánh catch lồng
        doThrow(new RuntimeException("fail1")).when(userActivityLogRepository).save(any());
        // Ném tiếp exception khi log.error
        org.slf4j.Logger logger = mock(org.slf4j.Logger.class);
        doThrow(new RuntimeException("fail2")).when(logger).error(anyString(), any(), any());
        // Gán logger cho aspect nếu có thể (nếu dùng lombok thì bỏ qua, chỉ cần test save throw là đủ)
        var method = aspect.getClass().getDeclaredMethod("saveFailedLogEntry", JoinPoint.class, UserActivity.class, String.class);
        method.setAccessible(true);
        method.invoke(aspect, joinPoint, userActivity, "err");
    }

    @Test
    void testLogActivitySuccess_nullLocationOrDevice() throws Exception {
        UserActivityLog log = new UserActivityLog();
        log.setFullName("Test User");
        log.setIpAddress("127.0.0.1");
        log.setUserAgent("Mozilla");
        when(userActivity.activityType()).thenReturn(com.example.demologin.enums.ActivityType.LOGIN_SUCCESS);
        // locationUtil.getLocationFromIP trả về null
        when(locationUtil.getLocationFromIP(any())).thenReturn(null);
        try (var userAgentMocked = org.mockito.Mockito.mockStatic(com.example.demologin.utils.UserAgentUtil.class);
             var locationUtilMocked = org.mockito.Mockito.mockStatic(com.example.demologin.utils.LocationUtil.class)) {
            userAgentMocked.when(() -> com.example.demologin.utils.UserAgentUtil.parseUserAgent(any())).thenReturn(null);
            // formatLocationInfo trả về null
            locationUtilMocked.when(() -> com.example.demologin.utils.LocationUtil.formatLocationInfo(null)).thenReturn(null);
            userAgentMocked.when(() -> com.example.demologin.utils.UserAgentUtil.formatDeviceInfo(null)).thenReturn(null);
            var method = aspect.getClass().getDeclaredMethod("logActivitySuccess", UserActivity.class, UserActivityLog.class);
            method.setAccessible(true);
            method.invoke(aspect, userActivity, log);
        }
    }
    @Test
    void testFindExistingActivityLog_mapOrElseGetBranches() throws Exception {
        // Chuẩn bị ClientInfo
        var clientInfoCtor = aspect.getClass().getDeclaredClasses()[0].getDeclaredConstructor(String.class, String.class, String.class);
        clientInfoCtor.setAccessible(true);
        Object clientInfo = clientInfoCtor.newInstance("127.0.0.1", "Mozilla", "127.0.0.1");
        // userId != null, có log cũ
        UserActivityLog existingLog = new UserActivityLog();
        when(userActivityLogRepository.findTopByUserIdAndActivityTypeAndIpAddressAndUserAgentOrderByTimestampDesc(any(), any(), any(), any())).thenReturn(existingLog);
        var method = aspect.getClass().getDeclaredMethod("findExistingActivityLog", Long.class, UserActivity.class, aspect.getClass().getDeclaredClasses()[0]);
        method.setAccessible(true);
        Object result = method.invoke(aspect, 1L, userActivity, clientInfo);
        assertTrue(result instanceof java.util.Optional);
        assertTrue(((java.util.Optional<?>) result).isPresent());
        // userId != null, không có log cũ
        when(userActivityLogRepository.findTopByUserIdAndActivityTypeAndIpAddressAndUserAgentOrderByTimestampDesc(any(), any(), any(), any())).thenReturn(null);
        result = method.invoke(aspect, 1L, userActivity, clientInfo);
        assertTrue(result instanceof java.util.Optional);
        assertFalse(((java.util.Optional<?>) result).isPresent());
    }

    @Test
    void testUpdateExistingLog_nullDeviceOrLocation() throws Exception {
        UserActivityLog log = new UserActivityLog();
        log.setIpAddress("127.0.0.1");
        log.setFullName("old");
        var method = aspect.getClass().getDeclaredMethod("updateExistingLog", UserActivityLog.class, String.class, String.class, com.example.demologin.utils.UserAgentUtil.DeviceInfo.class, com.example.demologin.utils.LocationUtil.LocationInfo.class);
        method.setAccessible(true);
        // deviceInfo null
        com.example.demologin.utils.LocationUtil.LocationInfo locationInfo = new com.example.demologin.utils.LocationUtil.LocationInfo("Hanoi", "HN", "VN", "VN");
    Exception ex1 = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> method.invoke(aspect, log, "newName", "details", null, locationInfo));
    assertTrue(ex1.getCause() instanceof NullPointerException);
    // locationInfo null
    com.example.demologin.utils.UserAgentUtil.DeviceInfo deviceInfo = mock(com.example.demologin.utils.UserAgentUtil.DeviceInfo.class);
    when(deviceInfo.getBrowser()).thenReturn("Chrome");
    when(deviceInfo.getBrowserVersion()).thenReturn("1.0");
    when(deviceInfo.getOperatingSystem()).thenReturn("Win");
    when(deviceInfo.getDevice()).thenReturn("PC");
    when(deviceInfo.getDeviceType()).thenReturn("Desktop");
    Exception ex2 = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> method.invoke(aspect, log, "newName", "details", deviceInfo, null));
    assertTrue(ex2.getCause() instanceof NullPointerException);
    }

    @Test
    void testCreateNewActivityLog_nullDeviceOrLocation() throws Exception {
        var method = aspect.getClass().getDeclaredMethod("createNewActivityLog", UserActivity.class, Long.class, String.class, String.class, String.class, aspect.getClass().getDeclaredClasses()[0], com.example.demologin.utils.UserAgentUtil.DeviceInfo.class, com.example.demologin.utils.LocationUtil.LocationInfo.class);
        method.setAccessible(true);
        var clientInfoCtor = aspect.getClass().getDeclaredClasses()[0].getDeclaredConstructor(String.class, String.class, String.class);
        clientInfoCtor.setAccessible(true);
        Object clientInfo = clientInfoCtor.newInstance("127.0.0.1", "Mozilla", "127.0.0.1");
        // deviceInfo null
        com.example.demologin.utils.LocationUtil.LocationInfo locationInfo = new com.example.demologin.utils.LocationUtil.LocationInfo("Hanoi", "HN", "VN", "VN");
    Exception ex1 = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> method.invoke(aspect, userActivity, 1L, "FullName", "SUCCESS", "details", clientInfo, null, locationInfo));
    assertTrue(ex1.getCause() instanceof NullPointerException);
    // locationInfo null
    com.example.demologin.utils.UserAgentUtil.DeviceInfo deviceInfo = mock(com.example.demologin.utils.UserAgentUtil.DeviceInfo.class);
    when(deviceInfo.getBrowser()).thenReturn("Chrome");
    when(deviceInfo.getBrowserVersion()).thenReturn("1.0");
    when(deviceInfo.getOperatingSystem()).thenReturn("Win");
    when(deviceInfo.getDevice()).thenReturn("PC");
    when(deviceInfo.getDeviceType()).thenReturn("Desktop");
    Exception ex2 = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> method.invoke(aspect, userActivity, 1L, "FullName", "SUCCESS", "details", clientInfo, deviceInfo, null));
    assertTrue(ex2.getCause() instanceof NullPointerException);
    }
    @Test
    void testUpdateExistingLog_allBranches() throws Exception {
        UserActivityLog log = new UserActivityLog();
        log.setIpAddress("127.0.0.1");
        log.setFullName("old");
        com.example.demologin.utils.UserAgentUtil.DeviceInfo deviceInfo = mock(com.example.demologin.utils.UserAgentUtil.DeviceInfo.class);
        when(deviceInfo.getBrowser()).thenReturn("Chrome");
        when(deviceInfo.getBrowserVersion()).thenReturn("1.0");
        when(deviceInfo.getOperatingSystem()).thenReturn("Win");
        when(deviceInfo.getDevice()).thenReturn("PC");
        when(deviceInfo.getDeviceType()).thenReturn("Desktop");
        com.example.demologin.utils.LocationUtil.LocationInfo locationInfo = new com.example.demologin.utils.LocationUtil.LocationInfo("Hanoi", "HN", "VN", "VN");
        var method = aspect.getClass().getDeclaredMethod("updateExistingLog", UserActivityLog.class, String.class, String.class, com.example.demologin.utils.UserAgentUtil.DeviceInfo.class, com.example.demologin.utils.LocationUtil.LocationInfo.class);
        method.setAccessible(true);
        Object result = method.invoke(aspect, log, "newName", "details", deviceInfo, locationInfo);
        assertNotNull(result);
    }

    @Test
    void testUpdateDeviceInfo_allBranches() throws Exception {
        UserActivityLog log = new UserActivityLog();
        com.example.demologin.utils.UserAgentUtil.DeviceInfo deviceInfo = mock(com.example.demologin.utils.UserAgentUtil.DeviceInfo.class);
        when(deviceInfo.getBrowser()).thenReturn("Chrome");
        when(deviceInfo.getBrowserVersion()).thenReturn("1.0");
        when(deviceInfo.getOperatingSystem()).thenReturn("Win");
        when(deviceInfo.getDevice()).thenReturn("PC");
        when(deviceInfo.getDeviceType()).thenReturn("Desktop");
        var method = aspect.getClass().getDeclaredMethod("updateDeviceInfo", UserActivityLog.class, com.example.demologin.utils.UserAgentUtil.DeviceInfo.class);
        method.setAccessible(true);
        method.invoke(aspect, log, deviceInfo);
        // test null deviceInfo throws NPE
    Exception ex = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> method.invoke(aspect, log, (Object) null));
    assertTrue(ex.getCause() instanceof NullPointerException);
    }

    @Test
    void testUpdateLocationInfo_allBranches() throws Exception {
        UserActivityLog log = new UserActivityLog();
        com.example.demologin.utils.LocationUtil.LocationInfo locationInfo = new com.example.demologin.utils.LocationUtil.LocationInfo("Hanoi", "HN", "VN", "VN");
        var method = aspect.getClass().getDeclaredMethod("updateLocationInfo", UserActivityLog.class, com.example.demologin.utils.LocationUtil.LocationInfo.class);
        method.setAccessible(true);
        method.invoke(aspect, log, locationInfo);
        // test null locationInfo throws NPE
    Exception ex = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> method.invoke(aspect, log, (Object) null));
    assertTrue(ex.getCause() instanceof NullPointerException);
    }

    @Test
    void testCreateNewActivityLog_allBranches() throws Exception {
        UserActivityLog log = new UserActivityLog();
        com.example.demologin.utils.UserAgentUtil.DeviceInfo deviceInfo = mock(com.example.demologin.utils.UserAgentUtil.DeviceInfo.class);
        when(deviceInfo.getBrowser()).thenReturn("Chrome");
        when(deviceInfo.getBrowserVersion()).thenReturn("1.0");
        when(deviceInfo.getOperatingSystem()).thenReturn("Win");
        when(deviceInfo.getDevice()).thenReturn("PC");
        when(deviceInfo.getDeviceType()).thenReturn("Desktop");
        com.example.demologin.utils.LocationUtil.LocationInfo locationInfo = new com.example.demologin.utils.LocationUtil.LocationInfo("Hanoi", "HN", "VN", "VN");
        var method = aspect.getClass().getDeclaredMethod("createNewActivityLog", UserActivity.class, Long.class, String.class, String.class, String.class, aspect.getClass().getDeclaredClasses()[0], com.example.demologin.utils.UserAgentUtil.DeviceInfo.class, com.example.demologin.utils.LocationUtil.LocationInfo.class);
        method.setAccessible(true);
        // Tạo ClientInfo qua reflection
        var clientInfoCtor = aspect.getClass().getDeclaredClasses()[0].getDeclaredConstructor(String.class, String.class, String.class);
        clientInfoCtor.setAccessible(true);
        Object clientInfo = clientInfoCtor.newInstance("127.0.0.1", "Mozilla", "127.0.0.1");
        Object result = method.invoke(aspect, userActivity, 1L, "FullName", "SUCCESS", "details", clientInfo, deviceInfo, locationInfo);
        assertNotNull(result);
    }

    @Test
    void testFindExistingActivityLog_branches() throws Exception {
        var method = aspect.getClass().getDeclaredMethod("findExistingActivityLog", Long.class, UserActivity.class, aspect.getClass().getDeclaredClasses()[0]);
        method.setAccessible(true);
        // userId null
    var clientInfoCtor = aspect.getClass().getDeclaredClasses()[0].getDeclaredConstructor(String.class, String.class, String.class);
    clientInfoCtor.setAccessible(true);
    Object clientInfo = clientInfoCtor.newInstance("127.0.0.1", "Mozilla", "127.0.0.1");
        Object result = method.invoke(aspect, null, userActivity, clientInfo);
        assertTrue(result instanceof java.util.Optional);
        // userId có giá trị
        when(userActivityLogRepository.findTopByUserIdAndActivityTypeAndIpAddressAndUserAgentOrderByTimestampDesc(any(), any(), any(), any())).thenReturn(new UserActivityLog());
        result = method.invoke(aspect, 1L, userActivity, clientInfo);
        assertTrue(result instanceof java.util.Optional);
    }
    @Test
    void testExtractUserFromLoginRequest_noLoginRequest() throws Exception {
        Object[] args = new Object[] {"not a login request"};
        when(joinPoint.getArgs()).thenReturn(args);
        var method = aspect.getClass().getDeclaredMethod("extractUserFromLoginRequest", JoinPoint.class);
        method.setAccessible(true);
        try {
            method.invoke(aspect, joinPoint);
            fail("Expected exception");
        } catch (Exception e) {
            Throwable cause = e.getCause();
            assertNotNull(cause);
            assertEquals(com.example.demologin.exception.exceptions.UserActivityLoggingException.class, cause.getClass());
        }
    }

    @Test
    void testExtractUserFromLoginRequest_anonymousUser() throws Exception {
        com.example.demologin.dto.request.login.LoginRequest loginRequest = mock(com.example.demologin.dto.request.login.LoginRequest.class);
        when(loginRequest.getUsername()).thenReturn("anonymousUser");
        when(joinPoint.getArgs()).thenReturn(new Object[]{loginRequest});
        var method = aspect.getClass().getDeclaredMethod("extractUserFromLoginRequest", JoinPoint.class);
        method.setAccessible(true);
        assertThrows(com.example.demologin.exception.exceptions.UserActivityLoggingException.class, () -> {
            try {
                method.invoke(aspect, joinPoint);
            } catch (Exception e) {
                throw e.getCause();
            }
        });
    }

    @Test
    void testExtractUserFromLoginRequest_userNotFound() throws Exception {
        com.example.demologin.dto.request.login.LoginRequest loginRequest = mock(com.example.demologin.dto.request.login.LoginRequest.class);
        when(loginRequest.getUsername()).thenReturn("notfound");
        when(joinPoint.getArgs()).thenReturn(new Object[]{loginRequest});
        try (var emailUtilsMocked = org.mockito.Mockito.mockStatic(com.example.demologin.utils.EmailUtils.class)) {
            emailUtilsMocked.when(() -> com.example.demologin.utils.EmailUtils.isValidEmail(any())).thenReturn(false);
            when(userRepository.findByUsername(eq("notfound"))).thenReturn(java.util.Optional.empty());
            var method = aspect.getClass().getDeclaredMethod("extractUserFromLoginRequest", JoinPoint.class);
            method.setAccessible(true);
            try {
                method.invoke(aspect, joinPoint);
                fail("Expected exception");
            } catch (Exception e) {
                Throwable cause = e.getCause();
                assertNotNull(cause);
                assertEquals(com.example.demologin.exception.exceptions.UserActivityLoggingException.class, cause.getClass());
            }
        }
    }

    @Test
    void testGetCurrentUserOrFromLoginAttempt_loginAttemptException() throws Exception {
        when(userActivity.activityType()).thenReturn(com.example.demologin.enums.ActivityType.LOGIN_ATTEMPT);
        var method = aspect.getClass().getDeclaredMethod("getCurrentUserOrFromLoginAttempt", JoinPoint.class, UserActivity.class, Object.class);
        method.setAccessible(true);
        // extractUserFromLoginRequest sẽ throw
        when(joinPoint.getArgs()).thenReturn(new Object[] {"not a login request"});
        Object result = method.invoke(aspect, joinPoint, userActivity, null);
        assertNull(result);
    }

    @Test
    void testGetCurrentUserOrFromLoginAttempt_otherException() throws Exception {
        when(userActivity.activityType()).thenReturn(com.example.demologin.enums.ActivityType.LOGIN_SUCCESS);
        when(accountUtils.getCurrentUser()).thenThrow(new com.example.demologin.exception.exceptions.UserNotAuthenticatedException("not auth"));
        var method = aspect.getClass().getDeclaredMethod("getCurrentUserOrFromLoginAttempt", JoinPoint.class, UserActivity.class, Object.class);
        method.setAccessible(true);
        Object result = method.invoke(aspect, joinPoint, userActivity, null);
        assertNull(result);
    }

    @Test
    void testCreateOrUpdateActivityLog_updateExisting() throws Exception {
        User user = new User();
        user.setUserId(1L);
        user.setFullName("Test User");
        when(userActivity.logUserId()).thenReturn(true);
        when(userActivity.details()).thenReturn("");
        when(userActivity.activityType()).thenReturn(com.example.demologin.enums.ActivityType.LOGIN_SUCCESS);
        // Mock client info
        when(ipUtils.getClientIpAddress()).thenReturn("127.0.0.1");
        when(ipUtils.getUserAgent()).thenReturn("Mozilla");
        try (var userAgentMocked = org.mockito.Mockito.mockStatic(com.example.demologin.utils.UserAgentUtil.class)) {
            userAgentMocked.when(() -> com.example.demologin.utils.UserAgentUtil.parseUserAgent(any())).thenReturn(mock(com.example.demologin.utils.UserAgentUtil.DeviceInfo.class));
            when(locationUtil.getLocationFromIP(any())).thenReturn(mock(com.example.demologin.utils.LocationUtil.LocationInfo.class));
            UserActivityLog existingLog = mock(UserActivityLog.class);
            when(userActivityLogRepository.findTopByUserIdAndActivityTypeAndIpAddressAndUserAgentOrderByTimestampDesc(any(), any(), any(), any())).thenReturn(existingLog);
            var method = aspect.getClass().getDeclaredMethod("createOrUpdateActivityLog", JoinPoint.class, UserActivity.class, User.class);
            method.setAccessible(true);
            Object result = method.invoke(aspect, joinPoint, userActivity, user);
            assertNotNull(result);
        }
    }

    @Test
    void testCreateOrUpdateActivityLog_createNew() throws Exception {
        User user = new User();
        user.setUserId(1L);
        user.setFullName("Test User");
        when(userActivity.logUserId()).thenReturn(true);
        when(userActivity.details()).thenReturn("");
        when(userActivity.activityType()).thenReturn(com.example.demologin.enums.ActivityType.LOGIN_SUCCESS);
        when(ipUtils.getClientIpAddress()).thenReturn("127.0.0.1");
        when(ipUtils.getUserAgent()).thenReturn("Mozilla");
        try (var userAgentMocked = org.mockito.Mockito.mockStatic(com.example.demologin.utils.UserAgentUtil.class)) {
            userAgentMocked.when(() -> com.example.demologin.utils.UserAgentUtil.parseUserAgent(any())).thenReturn(mock(com.example.demologin.utils.UserAgentUtil.DeviceInfo.class));
            when(locationUtil.getLocationFromIP(any())).thenReturn(mock(com.example.demologin.utils.LocationUtil.LocationInfo.class));
            when(userActivityLogRepository.findTopByUserIdAndActivityTypeAndIpAddressAndUserAgentOrderByTimestampDesc(any(), any(), any(), any())).thenReturn(null);
            var method = aspect.getClass().getDeclaredMethod("createOrUpdateActivityLog", JoinPoint.class, UserActivity.class, User.class);
            method.setAccessible(true);
            Object result = method.invoke(aspect, joinPoint, userActivity, user);
            assertNotNull(result);
        }
    }

    @Test
    void testSaveFailedLogEntry_exception() throws Exception {
        doThrow(new RuntimeException("fail")).when(userActivityLogRepository).save(any());
        var method = aspect.getClass().getDeclaredMethod("saveFailedLogEntry", JoinPoint.class, UserActivity.class, String.class);
        method.setAccessible(true);
        method.invoke(aspect, joinPoint, userActivity, "err");
    }

    @Test
    void testFormatIpAddress_branches() throws Exception {
        var method = aspect.getClass().getDeclaredMethod("formatIpAddress", String.class);
        method.setAccessible(true);
        assertEquals("unknown", method.invoke(aspect, (Object) null));
        assertEquals("127.0.0.1 (localhost)", method.invoke(aspect, "127.0.0.1"));
        assertEquals("::1 (localhost)", method.invoke(aspect, "::1"));
        assertEquals("192.168.1.1", method.invoke(aspect, "192.168.1.1"));
    }

    @Test
    void testGetIpForLocation_branches() throws Exception {
        var method = aspect.getClass().getDeclaredMethod("getIpForLocation", String.class);
        method.setAccessible(true);
        assertEquals("8.8.8.8", method.invoke(aspect, "127.0.0.1"));
        assertEquals("8.8.8.8", method.invoke(aspect, "::1"));
        assertEquals("192.168.1.1", method.invoke(aspect, "192.168.1.1"));
        assertEquals("127.0.0.1", method.invoke(aspect, "127.0.0.1 (localhost)"));
    }

    @Test
    void testGetDetails_branches() throws Exception {
        when(userActivity.details()).thenReturn("");
        when(userActivity.activityType()).thenReturn(com.example.demologin.enums.ActivityType.LOGIN_SUCCESS);
        when(signature.getName()).thenReturn("testMethod");
        var method = aspect.getClass().getDeclaredMethod("getDetails", UserActivity.class, JoinPoint.class);
        method.setAccessible(true);
        assertTrue(method.invoke(aspect, userActivity, joinPoint).toString().contains("LOGIN_SUCCESS"));
        when(userActivity.details()).thenReturn("custom");
        assertEquals("custom", method.invoke(aspect, userActivity, joinPoint));
    }

    @Test
    void testLogActivitySuccess_branches() throws Exception {
        UserActivityLog log = new UserActivityLog();
        log.setFullName(null);
        log.setIpAddress("127.0.0.1");
        log.setUserAgent("Mozilla");
        when(userActivity.activityType()).thenReturn(com.example.demologin.enums.ActivityType.LOGIN_SUCCESS);
        com.example.demologin.utils.LocationUtil.LocationInfo locationInfo = new com.example.demologin.utils.LocationUtil.LocationInfo("Hanoi", "HN", "VN", "VN");
        when(locationUtil.getLocationFromIP(any())).thenReturn(locationInfo);
        try (var userAgentMocked = org.mockito.Mockito.mockStatic(com.example.demologin.utils.UserAgentUtil.class)) {
            userAgentMocked.when(() -> com.example.demologin.utils.UserAgentUtil.parseUserAgent(any())).thenReturn(mock(com.example.demologin.utils.UserAgentUtil.DeviceInfo.class));
            var method = aspect.getClass().getDeclaredMethod("logActivitySuccess", UserActivity.class, UserActivityLog.class);
            method.setAccessible(true);
            method.invoke(aspect, userActivity, log);
            log.setFullName("Test User");
            method.invoke(aspect, userActivity, log);
        }
    }
    @Mock UserActivityLogRepository userActivityLogRepository;
    @Mock UserRepository userRepository;
    @Mock AccountUtils accountUtils;
    @Mock IpUtilsWrapper ipUtils;
    @Mock UserAgentUtil userAgentUtil;
    @Mock LocationUtil locationUtil;
    @Mock JoinPoint joinPoint;
    @Mock org.aspectj.lang.Signature signature;
    @Mock UserActivity userActivity;

    UserActivityAspect aspect;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    aspect = new UserActivityAspect(userActivityLogRepository, userRepository, accountUtils, ipUtils, userAgentUtil, locationUtil);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getName()).thenReturn("testMethod");
    }

    @Test
    void testLogUserActivity_success() {
        User user = new User();
        when(accountUtils.getCurrentUser()).thenReturn(user);
        when(userActivity.activityType()).thenReturn(com.example.demologin.enums.ActivityType.LOGIN_SUCCESS);
        when(userActivity.details()).thenReturn("");
        when(userActivity.logUserId()).thenReturn(true);
        when(ipUtils.getClientIpAddress()).thenReturn("127.0.0.1");
        when(ipUtils.getUserAgent()).thenReturn("Mozilla");
        // Mock static methods for UserAgentUtil and LocationUtil
        try (var userAgentMocked = org.mockito.Mockito.mockStatic(com.example.demologin.utils.UserAgentUtil.class)) {
            userAgentMocked.when(() -> com.example.demologin.utils.UserAgentUtil.parseUserAgent(any())).thenReturn(null);
            when(locationUtil.getLocationFromIP(any())).thenReturn(null);
            aspect.logUserActivity(joinPoint, userActivity, null);
        }
        verify(userActivityLogRepository, atLeastOnce()).save(any(UserActivityLog.class));
    }
}
