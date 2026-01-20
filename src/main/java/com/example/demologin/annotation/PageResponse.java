package com.example.demologin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to automatically convert Page<T> response to PageResponse<T> format
 * Apply this to controller methods that return Page<T> to get standardized pagination response
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PageResponse {
}
