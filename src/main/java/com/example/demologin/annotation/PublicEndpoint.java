package com.example.demologin.annotation;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import java.lang.annotation.*;

/**
 * Annotation để đánh dấu các endpoint không cần authentication
 * Các method được đánh dấu với annotation này sẽ tự động được permit all
 * trong SecurityFilterChain mà không cần khai báo thủ công
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SecurityRequirements(value = {}) // QUAN TRỌNG: Xóa security requirements
public @interface PublicEndpoint {

}
