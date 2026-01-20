package com.example.demologin.annotation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SecurityRequirement(name = "bearerAuth")
public @interface AuthenticatedEndpoint {
}
