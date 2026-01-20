package com.example.demologin.aspect;

import com.example.demologin.annotation.PageResponse;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Use fully qualified name for PageResponse to match aspect return type

class PageResponseAspectTest {
    PageResponseAspect aspect;

    @Mock
    MethodParameter methodParameter;
    @Mock
    HttpMessageConverter<?> converter;
    @Mock
    ServerHttpRequest request;
    @Mock
    ServerHttpResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aspect = new PageResponseAspect();
    }

    @Test
    void testSupports_withAnnotation() {
        when(methodParameter.hasMethodAnnotation(PageResponse.class)).thenReturn(true);
    assertTrue(aspect.supports(methodParameter, MappingJackson2HttpMessageConverter.class));
    }

    @Test
    void testSupports_withoutAnnotation() {
        when(methodParameter.hasMethodAnnotation(PageResponse.class)).thenReturn(false);
    assertFalse(aspect.supports(methodParameter, MappingJackson2HttpMessageConverter.class));
    }

    @Test
    void testBeforeBodyWrite_withPage() {
        org.springframework.data.domain.Page<String> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of("a", "b"));
        when(methodParameter.hasMethodAnnotation(PageResponse.class)).thenReturn(true);
        Object result = aspect.beforeBodyWrite(page, methodParameter, MediaType.APPLICATION_JSON, MappingJackson2HttpMessageConverter.class, request, response);
        assertTrue(result instanceof com.example.demologin.dto.response.PageResponse);
    }

    @Test
    void testBeforeBodyWrite_withNonPage() {
        String body = "not a page";
        when(methodParameter.hasMethodAnnotation(PageResponse.class)).thenReturn(true);
    Object result = aspect.beforeBodyWrite(body, methodParameter, MediaType.APPLICATION_JSON, MappingJackson2HttpMessageConverter.class, request, response);
    assertEquals(body, result);
    }
}
