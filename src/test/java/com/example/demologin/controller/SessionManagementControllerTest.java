package com.example.demologin.controller;

import com.example.demologin.service.SessionManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SessionManagementControllerTest {
    @Mock
    private SessionManagementService sessionManagementService;

    @InjectMocks
    private SessionManagementController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void logoutCurrentDevice() {
        doNothing().when(sessionManagementService).logoutCurrentDevice();
        Object result = controller.logoutCurrentDevice();
        assertNull(result);
        verify(sessionManagementService).logoutCurrentDevice();
    }

    @Test
    void logoutFromAllDevices() {
        doNothing().when(sessionManagementService).logoutFromAllDevices();
        Object result = controller.logoutFromAllDevices();
        assertNull(result);
        verify(sessionManagementService).logoutFromAllDevices();
    }

    @Test
    void forceLogoutUser() {
        doNothing().when(sessionManagementService).forceLogoutUser(7L);
        Object result = controller.forceLogoutUser(7L);
        assertNull(result);
        verify(sessionManagementService).forceLogoutUser(7L);
    }
}
