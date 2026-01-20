package com.example.demologin.cache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CacheDetectorTest {
    @Test
    void testGetRelatedCacheKeys_updateOtherType() throws Exception {
        setMethod("update", Integer.class);
        Mockito.when(joinPoint.getArgs()).thenReturn(new Object[]{42});
        Set<String> keys = detector.getRelatedCacheKeys(joinPoint);
        // Chỉ có getAll, không có getById
        assertTrue(keys.contains("DummyController:getAll"));
        assertEquals(1, keys.size());
    }
    @Test
    void testGetRelatedCacheKeys_deleteNoArgs2() throws Exception {
        setMethod("delete");
        Mockito.when(joinPoint.getArgs()).thenReturn(new Object[]{});
        Set<String> keys = detector.getRelatedCacheKeys(joinPoint);
        assertTrue(keys.contains("DummyController:getAll"));
        assertEquals(1, keys.size());
    }
    @Test
    void testGetRelatedCacheKeys_deleteStringNumeric() throws Exception {
        setMethod("delete", String.class);
        Mockito.when(joinPoint.getArgs()).thenReturn(new Object[]{"789"});
        Set<String> keys = detector.getRelatedCacheKeys(joinPoint);
        assertTrue(keys.contains("DummyController:getAll"));
        assertTrue(keys.contains("DummyController:getById:789"));
    }
    @Test
    void testGetRelatedCacheKeys_updateStringNumeric() throws Exception {
        setMethod("update", String.class);
        Mockito.when(joinPoint.getArgs()).thenReturn(new Object[]{"123"});
        Set<String> keys = detector.getRelatedCacheKeys(joinPoint);
        assertTrue(keys.contains("DummyController:getAll"));
        assertTrue(keys.contains("DummyController:getById:123"));
    }
    @Test
    void testGetRelatedCacheKeys_updateStringNonNumeric() throws Exception {
        setMethod("update", String.class);
        Mockito.when(joinPoint.getArgs()).thenReturn(new Object[]{"abc"});
        Set<String> keys = detector.getRelatedCacheKeys(joinPoint);
        assertTrue(keys.contains("DummyController:getAll"));
        assertTrue(keys.contains("DummyController:getById:abc"));
    }
    @Test
    void testGetRelatedCacheKeys_updateNoArgs() throws Exception {
        setMethod("update");
        Mockito.when(joinPoint.getArgs()).thenReturn(new Object[]{});
        Set<String> keys = detector.getRelatedCacheKeys(joinPoint);
        assertTrue(keys.contains("DummyController:getAll"));
        assertEquals(1, keys.size());
    }

    @Test
    void testGetRelatedCacheKeys_deleteNoArgs() throws Exception {
        setMethod("delete");
        Mockito.when(joinPoint.getArgs()).thenReturn(new Object[]{});
        Set<String> keys = detector.getRelatedCacheKeys(joinPoint);
        assertTrue(keys.contains("DummyController:getAll"));
        assertEquals(1, keys.size());
    }
    private CacheDetector detector;
    private ProceedingJoinPoint joinPoint;
    private MethodSignature signature;
    private Method method;

    static class DummyController {
        @GetMapping
        public void getAll() {}
        @PostMapping
        public void create() {}
    @PutMapping
    public void update(Long id) {}
    @PutMapping
    public void update(String id) {}
    @PutMapping
    public void update(Integer id) {}
        @PutMapping
        public void update() {}
        @DeleteMapping
        public void delete(String id) {}
        @DeleteMapping
        public void delete() {}
        @PatchMapping
        public void patch() {}
        public void custom(Long id) {}
    }

    @BeforeEach
    void setUp() throws Exception {
        detector = new CacheDetector();
        joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        signature = Mockito.mock(MethodSignature.class);
        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
    }

    private void setMethod(String name, Class<?>... paramTypes) throws Exception {
        method = DummyController.class.getMethod(name, paramTypes);
        Mockito.when(signature.getMethod()).thenReturn(method);
        Mockito.when(signature.getDeclaringType()).thenReturn(DummyController.class);
        Mockito.when(signature.getMethod()).thenReturn(method);
    }

    @Test
    void testIsWriteOperation() throws Exception {
        setMethod("create");
        assertTrue(detector.isWriteOperation(joinPoint));
        setMethod("update", Long.class);
        assertTrue(detector.isWriteOperation(joinPoint));
        setMethod("delete", String.class);
        assertTrue(detector.isWriteOperation(joinPoint));
        setMethod("patch");
        assertTrue(detector.isWriteOperation(joinPoint));
        setMethod("getAll");
        assertFalse(detector.isWriteOperation(joinPoint));
        setMethod("custom", Long.class);
        assertFalse(detector.isWriteOperation(joinPoint));
    }

    @Test
    void testGenerateCacheKey() throws Exception {
        setMethod("getAll");
        assertEquals("DummyController:getAll", detector.generateCacheKey(joinPoint));
        setMethod("update", Long.class);
        assertEquals("DummyController:update", detector.generateCacheKey(joinPoint));
    }

    @Test
    void testGetRelatedCacheKey() throws Exception {
        setMethod("create");
        assertEquals("DummyController:getAll", detector.getRelatedCacheKey(joinPoint));
        setMethod("update", Long.class);
        assertEquals("DummyController:getAll", detector.getRelatedCacheKey(joinPoint));
        setMethod("delete", String.class);
        assertEquals("DummyController:getAll", detector.getRelatedCacheKey(joinPoint));
        setMethod("getAll");
        assertEquals("DummyController:getAll", detector.getRelatedCacheKey(joinPoint));
        setMethod("custom", Long.class);
        assertEquals("DummyController:custom", detector.getRelatedCacheKey(joinPoint));
    }

    @Test
    void testGetRelatedCacheKeys_updateLong() throws Exception {
        setMethod("update", Long.class);
        Mockito.when(joinPoint.getArgs()).thenReturn(new Object[]{123L});
        Set<String> keys = detector.getRelatedCacheKeys(joinPoint);
        assertTrue(keys.contains("DummyController:getAll"));
        assertTrue(keys.contains("DummyController:getById:123"));
    }

    @Test
    void testGetRelatedCacheKeys_deleteString() throws Exception {
        setMethod("delete", String.class);
        Mockito.when(joinPoint.getArgs()).thenReturn(new Object[]{"456"});
        Set<String> keys = detector.getRelatedCacheKeys(joinPoint);
        assertTrue(keys.contains("DummyController:getAll"));
        assertTrue(keys.contains("DummyController:getById:456"));
    }

    @Test
    void testGetRelatedCacheKeys_deleteStringNonNumeric() throws Exception {
        setMethod("delete", String.class);
        Mockito.when(joinPoint.getArgs()).thenReturn(new Object[]{"abc"});
        Set<String> keys = detector.getRelatedCacheKeys(joinPoint);
        assertTrue(keys.contains("DummyController:getAll"));
        assertTrue(keys.contains("DummyController:getById:abc"));
    }

    @Test
    void testGetRelatedCacheKeys_create() throws Exception {
        setMethod("create");
        Mockito.when(joinPoint.getArgs()).thenReturn(new Object[]{});
        Set<String> keys = detector.getRelatedCacheKeys(joinPoint);
        assertTrue(keys.contains("DummyController:getAll"));
        assertEquals(1, keys.size());
    }

    @Test
    void testGetRelatedCacheKeys_custom() throws Exception {
        setMethod("custom", Long.class);
        Mockito.when(joinPoint.getArgs()).thenReturn(new Object[]{123L});
        Set<String> keys = detector.getRelatedCacheKeys(joinPoint);
        assertTrue(keys.contains("DummyController:getAll"));
        assertEquals(1, keys.size());
    }
}
