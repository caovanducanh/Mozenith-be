package com.example.demologin.aspect;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.dto.response.ResponseObject;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiResponseAspectTest {
    ApiResponseAspect aspect;

    @Mock
    ProceedingJoinPoint joinPoint;
    @Mock
    MethodSignature methodSignature;
    @Mock
    ApiResponse apiResponse;
    @Mock
    Method method;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aspect = new ApiResponseAspect();
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleApiResponse_withReturnValue() throws Throwable {
    when(apiResponse.message()).thenReturn("OK");
    doReturn(String.class).when(method).getReturnType();
    when(joinPoint.proceed()).thenReturn("data");
    when(method.isAnnotationPresent(org.mockito.ArgumentMatchers.any(Class.class))).thenReturn(false);
        Object result = aspect.handleApiResponse(joinPoint, apiResponse);
        assertTrue(result instanceof ResponseObject);
        ResponseObject ro = (ResponseObject) result;
        assertEquals(200, ro.getStatusCode());
        assertEquals("OK", ro.getMessage());
        assertEquals("data", ro.getData());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleApiResponse_withVoidReturn() throws Throwable {
    when(apiResponse.message()).thenReturn("Done");
    doReturn(Void.TYPE).when(method).getReturnType();
    when(joinPoint.proceed()).thenReturn(null);
    when(method.isAnnotationPresent(org.mockito.ArgumentMatchers.any(Class.class))).thenReturn(false);
        Object result = aspect.handleApiResponse(joinPoint, apiResponse);
        assertTrue(result instanceof ResponseObject);
        ResponseObject ro = (ResponseObject) result;
        assertEquals(200, ro.getStatusCode());
        assertEquals("Done", ro.getMessage());
        assertNull(ro.getData());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleApiResponse_withPostMapping() throws Throwable {
    when(apiResponse.message()).thenReturn("Created");
    doReturn(String.class).when(method).getReturnType();
    when(joinPoint.proceed()).thenReturn("created");
    when(method.isAnnotationPresent(org.mockito.ArgumentMatchers.any(Class.class))).thenReturn(false);
    when(method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class)).thenReturn(true);
        Object result = aspect.handleApiResponse(joinPoint, apiResponse);
        assertTrue(result instanceof ResponseObject);
        ResponseObject ro = (ResponseObject) result;
        assertEquals(HttpStatus.CREATED.value(), ro.getStatusCode());
        assertEquals("Created", ro.getMessage());
        assertEquals("created", ro.getData());
    }
}
