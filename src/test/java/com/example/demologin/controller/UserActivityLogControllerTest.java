package com.example.demologin.controller;

import com.example.demologin.service.UserActivityLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import org.springframework.data.domain.Page;
import com.example.demologin.dto.response.UserActivityLogResponse;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class UserActivityLogControllerTest {
    @Test
    void getActivityLogsByActionType() {
        Page<UserActivityLogResponse> page = new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), org.springframework.data.domain.PageRequest.of(0, 20), 0);
        when(userActivityLogService.getActivityLogsByType("LOGIN", 0, 20)).thenReturn(page);
        Object result = controller.getActivityLogsByActionType("LOGIN", 0, 20);
        assertEquals(page, result);
        verify(userActivityLogService).getActivityLogsByType("LOGIN", 0, 20);
    }

    @Test
    void getActivityLogsByDateRange() {
        Page<UserActivityLogResponse> page = new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), org.springframework.data.domain.PageRequest.of(0, 20), 0);
        // 2023-01-01 to 2023-01-02
        when(userActivityLogService.getActivityLogsByDateRange(
                java.time.LocalDate.parse("2023-01-01").atStartOfDay(),
                java.time.LocalDate.parse("2023-01-02").atTime(23, 59, 59),
                0, 20)).thenReturn(page);
        Object result = controller.getActivityLogsByDateRange("2023-01-01", "2023-01-02", 0, 20);
        assertEquals(page, result);
        verify(userActivityLogService).getActivityLogsByDateRange(
                java.time.LocalDate.parse("2023-01-01").atStartOfDay(),
                java.time.LocalDate.parse("2023-01-02").atTime(23, 59, 59),
                0, 20);
    }

    @Test
    void exportActivityLogs() {
        Page<UserActivityLogResponse> page = new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), org.springframework.data.domain.PageRequest.of(0, 20), 0);
        com.example.demologin.dto.request.userActivityLog.UserActivityLogExportRequest req = new com.example.demologin.dto.request.userActivityLog.UserActivityLogExportRequest();
        when(userActivityLogService.exportActivityLogs(req, 0, 20)).thenReturn(page);
        Object result = controller.exportActivityLogs(req, 0, 20);
        assertEquals(page, result);
        verify(userActivityLogService).exportActivityLogs(req, 0, 20);
    }

    @Test
    void getMyLoginHistory() {
        Page<UserActivityLogResponse> page = new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), org.springframework.data.domain.PageRequest.of(0, 20), 0);
        when(userActivityLogService.getMyLoginHistory(0, 20)).thenReturn(page);
        Object result = controller.getMyLoginHistory(0, 20);
        assertEquals(page, result);
        verify(userActivityLogService).getMyLoginHistory(0, 20);
    }

    @Test
    void deleteActivityLog() {
        when(userActivityLogService.deleteActivityLog(99L)).thenReturn("deleted");
        Object result = controller.deleteActivityLog(99L);
        assertEquals("deleted", result);
        verify(userActivityLogService).deleteActivityLog(99L);
    }
    @Mock
    private UserActivityLogService userActivityLogService;

    @InjectMocks
    private UserActivityLogController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllActivityLogs() {
        Page<UserActivityLogResponse> page = new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), org.springframework.data.domain.PageRequest.of(0, 20), 0);
        when(userActivityLogService.getAllActivityLogs(0, 20)).thenReturn(page);
        Object result = controller.getAllActivityLogs(0, 20);
        assertEquals(page, result);
        verify(userActivityLogService).getAllActivityLogs(0, 20);
    }

    @Test
    void getActivityLogById() {
        com.example.demologin.dto.response.UserActivityLogResponse resp = new com.example.demologin.dto.response.UserActivityLogResponse();
        when(userActivityLogService.getActivityLogById(123L)).thenReturn(resp);
        Object result = controller.getActivityLogById(123L);
        assertEquals(resp, result);
        verify(userActivityLogService).getActivityLogById(123L);
    }

    @Test
    void getActivityLogsByUserId() {
        Page<UserActivityLogResponse> page = new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), org.springframework.data.domain.PageRequest.of(0, 20), 0);
        when(userActivityLogService.getActivityLogsByUserId(1L, 0, 20)).thenReturn(page);
        Object result = controller.getActivityLogsByUserId(1L, 0, 20);
        assertEquals(page, result);
        verify(userActivityLogService).getActivityLogsByUserId(1L, 0, 20);
    }
}
