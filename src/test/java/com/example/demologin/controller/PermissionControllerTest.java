package com.example.demologin.controller;

import com.example.demologin.service.PermissionService;
import com.example.demologin.dto.request.PermissionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import com.example.demologin.dto.response.PermissionResponse;
import java.util.Collections;
import java.util.List;

class PermissionControllerTest {
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private PermissionController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAll() {
        List<PermissionResponse> list = Collections.emptyList();
        when(permissionService.getAll()).thenReturn(list);
        Object result = controller.getAll();
        assertEquals(list, result);
        verify(permissionService).getAll();
    }

    @Test
    void update() {
        PermissionRequest req = new PermissionRequest();
        PermissionResponse resp = new PermissionResponse();
        when(permissionService.updatePermissionName(1L, req)).thenReturn(resp);
        Object result = controller.update(1L, req);
        assertEquals(resp, result);
        verify(permissionService).updatePermissionName(1L, req);
    }
}
