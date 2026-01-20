package com.example.demologin.cache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

@Component
public class CacheDetector {

    private static final Set<Class<? extends Annotation>> WRITE_ANNOTATIONS = Set.of(
            PostMapping.class, PutMapping.class,
            PatchMapping.class, DeleteMapping.class
    );

    public boolean isWriteOperation(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return WRITE_ANNOTATIONS.stream().anyMatch(method::isAnnotationPresent);
    }

    public String generateCacheKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() +
                ":" +
                signature.getMethod().getName();
    }

    public String getRelatedCacheKey(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        if (method.getName().startsWith("create") ||
                method.getName().startsWith("update") ||
                method.getName().startsWith("delete")) {
            return method.getDeclaringClass().getSimpleName() + ":getAll";
        }
        return generateCacheKey(joinPoint);
    }

    public Set<String> getRelatedCacheKeys(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Set<String> relatedKeys = new HashSet<>();

        // Add getAll key
        relatedKeys.add(method.getDeclaringClass().getSimpleName() + ":getAll");

        // For methods that modify specific entities, also invalidate getById caches
        if (method.getName().startsWith("update") || method.getName().startsWith("delete")) {
            Object[] args = joinPoint.getArgs();
            if (args.length > 0) {
                // Handle different argument types
                if (args[0] instanceof Long id) {
                    relatedKeys.add(method.getDeclaringClass().getSimpleName() + ":getById:" + id);
                } else if (args[0] instanceof String stringId) {
                    try {
                        Long id = Long.parseLong(stringId);
                        relatedKeys.add(method.getDeclaringClass().getSimpleName() + ":getById:" + id);
                    } catch (NumberFormatException e) {
                        // If it's not a number, use string as is
                        relatedKeys.add(method.getDeclaringClass().getSimpleName() + ":getById:" + stringId);
                    }
                }
            }
        }

        // For create operations, only invalidate getAll
        if (method.getName().startsWith("create")) {
            relatedKeys.add(method.getDeclaringClass().getSimpleName() + ":getAll");
        }

        return relatedKeys;
    }
}