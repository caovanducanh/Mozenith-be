package com.example.demologin.controller;

import com.example.demologin.service.TokenVersionService;
import com.example.demologin.dto.request.DummyActionRequest;
import com.example.demologin.dto.response.ResponseObject;
import org.springframework.http.ResponseEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class TokenVersionControllerTest {
    @Test
    void getUserTokenVersionByUserId() {
        ResponseEntity<com.example.demologin.dto.response.ResponseObject> resp = ResponseEntity.ok(new com.example.demologin.dto.response.ResponseObject(200, "ok", 5));
        when(tokenVersionService.getUserTokenVersionByUserId(10L)).thenReturn(resp);
        Object result = controller.getUserTokenVersionByUserId(10L);
        assertEquals(resp, result);
        verify(tokenVersionService).getUserTokenVersionByUserId(10L);
    }
    @Mock
    private TokenVersionService tokenVersionService;

    @InjectMocks
    private TokenVersionController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void invalidateAllCurrentUserTokens() {
        ResponseEntity<ResponseObject> resp = ResponseEntity.ok(new ResponseObject(200, "ok", null));
        when(tokenVersionService.incrementCurrentUserTokenVersion()).thenReturn(resp);
        Object result = controller.invalidateAllCurrentUserTokens(new DummyActionRequest());
        assertEquals(resp, result);
        verify(tokenVersionService).incrementCurrentUserTokenVersion();
    }

    @Test
    void invalidateUserTokensByUserId() {
        ResponseEntity<ResponseObject> resp = ResponseEntity.ok(new ResponseObject(200, "done", null));
        when(tokenVersionService.incrementUserTokenVersionByUserId(5L)).thenReturn(resp);
        Object result = controller.invalidateUserTokensByUserId(5L, new DummyActionRequest());
        assertEquals(resp, result);
        verify(tokenVersionService).incrementUserTokenVersionByUserId(5L);
    }

    @Test
    void getCurrentUserTokenVersion() {
        ResponseEntity<ResponseObject> resp = ResponseEntity.ok(new ResponseObject(200, "token", 2));
        when(tokenVersionService.getCurrentUserTokenVersion()).thenReturn(resp);
        Object result = controller.getCurrentUserTokenVersion();
        assertEquals(resp, result);
        verify(tokenVersionService).getCurrentUserTokenVersion();
    }
}
