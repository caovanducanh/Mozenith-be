package com.example.demologin.aspect;

import com.example.demologin.annotation.PageResponse;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Aspect to automatically convert Page<T> to PageResponse<T>
 * when method is annotated with @PageResponse
 * 
 * This aspect intercepts responses from controller methods that return Page objects
 * and automatically wraps them in our custom PageResponse format for consistent API responses.
 */
@RestControllerAdvice
public class PageResponseAspect implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.hasMethodAnnotation(PageResponse.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // Convert Spring Data Page<T> to our custom PageResponse<T>
        if (body instanceof Page<?>) {
            return new com.example.demologin.dto.response.PageResponse<>((Page<?>) body);
        }
        return body;
    }
}
