package com.example.demologin.aspect;

import com.example.demologin.cache.CacheDetector;
import com.example.demologin.utils.JwtUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmartCacheAspectTest {
    // Dummy public method for methodSignature.getMethod() mock
    public void dummyMethod() {}

    @Mock
    CacheDetector detector;
    @Mock
    JwtUtil jwtUtil;
    @Mock
    ProceedingJoinPoint joinPoint;
    @Mock
    MethodSignature methodSignature;

    @InjectMocks
    SmartCacheAspect aspect;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aspect = new SmartCacheAspect(detector, jwtUtil);
    }

    @Test
    void testHandleCache_readCacheMiss() throws Throwable {
        when(detector.isWriteOperation(any())).thenReturn(false);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getDeclaringType()).thenReturn(SmartCacheAspectTest.class);
    when(joinPoint.getArgs()).thenReturn(new Object[]{"a", 1});
        when(methodSignature.getMethod()).thenReturn(SmartCacheAspectTest.class.getDeclaredMethod("dummyMethod"));
    when(joinPoint.proceed()).thenReturn("result");
        Object result = aspect.handleCache(joinPoint);
        assertEquals("result", result);
    }

    @Test
    void testHandleCache_writeOperation() throws Throwable {
        when(detector.isWriteOperation(any())).thenReturn(true);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getDeclaringType()).thenReturn(SmartCacheAspectTest.class);
    when(joinPoint.getArgs()).thenReturn(new Object[]{"a", 1});
        when(methodSignature.getMethod()).thenReturn(SmartCacheAspectTest.class.getDeclaredMethod("dummyMethod"));
    when(joinPoint.proceed()).thenReturn("writeResult");
        Object result = aspect.handleCache(joinPoint);
        assertEquals("writeResult", result);
    }

}
