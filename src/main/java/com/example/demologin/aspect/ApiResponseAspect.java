package com.example.demologin.aspect;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.dto.response.ResponseObject;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;

@Aspect
@Component
public class ApiResponseAspect {

    @Around("@annotation(apiResponse)")
    public Object handleApiResponse(ProceedingJoinPoint joinPoint, ApiResponse apiResponse) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object result = joinPoint.proceed();

        HttpStatus status = detectStatus(method);

        // Nếu method trả void hoặc null -> tự wrap luôn
        Object data = (method.getReturnType().equals(Void.TYPE) || result == null) ? null : result;

        return new ResponseObject(
                status.value(),
                apiResponse.message(),
                data
        );
    }

    private HttpStatus detectStatus(Method method) {
        if (method.isAnnotationPresent(PostMapping.class)) return HttpStatus.CREATED;
        if (method.isAnnotationPresent(DeleteMapping.class)) return HttpStatus.OK;
        if (method.isAnnotationPresent(PutMapping.class)) return HttpStatus.OK;
        if (method.isAnnotationPresent(PatchMapping.class)) return HttpStatus.OK;
        if (method.isAnnotationPresent(GetMapping.class)) return HttpStatus.OK;

        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping rm = method.getAnnotation(RequestMapping.class);
            if (rm.method().length > 0) {
                return switch (rm.method()[0]) {
                    case POST -> HttpStatus.CREATED;
                    case DELETE, GET, PUT, PATCH -> HttpStatus.OK;
                    default -> HttpStatus.OK;
                };
            }
        }

        return HttpStatus.OK; // fallback
    }
}
