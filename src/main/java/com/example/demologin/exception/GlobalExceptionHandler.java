package com.example.demologin.exception;

import com.example.demologin.dto.response.ResponseObject;
import com.example.demologin.exception.exceptions.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ResponseObject> handleUnauthorizedException(UnauthorizedException ex) {
        log.warn("Unauthorized exception: {}", ex.getMessage());
        ResponseObject response = new ResponseObject(
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ResponseObject> handleForbiddenException(ForbiddenException ex) {
        log.warn("Forbidden exception: {}", ex.getMessage());
        ResponseObject response = new ResponseObject(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ResponseObject> handleNotFoundException(NotFoundException ex) {
        log.warn("Not found exception: {}", ex.getMessage());
        ResponseObject response = new ResponseObject(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ResponseObject> handleConflictException(ConflictException ex) {
        log.warn("Conflict exception: {}", ex.getMessage());
        ResponseObject response = new ResponseObject(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ResponseObject> handleValidationException(ValidationException ex) {
        log.warn("Validation exception: {}", ex.getMessage());
        ResponseObject response = new ResponseObject(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseObject> handleBadRequestException(BadRequestException ex) {
        log.warn("Bad request exception: {}", ex.getMessage());
        ResponseObject response = new ResponseObject(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseObject> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        ResponseObject response = new ResponseObject(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<ResponseObject> handleInternalServerErrorException(InternalServerErrorException ex) {
        log.error("Internal server error: {}", ex.getMessage(), ex);
        ResponseObject response = new ResponseObject(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseObject> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ResponseObject response = new ResponseObject(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors
        );
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseObject> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("Constraint violation error: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        }
        
        ResponseObject response = new ResponseObject(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors
        );
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseObject> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ResponseObject response = new ResponseObject(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                null
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ResponseObject> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ResponseObject(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), null));
    }

    @ExceptionHandler(InvalidPrincipalTypeException.class)
    public ResponseEntity<ResponseObject> handleInvalidPrincipalType(InvalidPrincipalTypeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST) // hoặc UNAUTHORIZED nếu phù hợp hơn
                .body(new ResponseObject(
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(UserNotAuthenticatedException.class)
    public ResponseEntity<ResponseObject> handleUserNotAuthenticated(UserNotAuthenticatedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ResponseObject(
                        HttpStatus.UNAUTHORIZED.value(),
                        ex.getMessage(),
                        null
                ));
    }
    @ExceptionHandler(UserActivityLoggingException.class)
    public ResponseEntity<ResponseObject> handleUserActivityLogging(UserActivityLoggingException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseObject(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        ex.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(CacheInvocationException.class)
    public ResponseEntity<ResponseObject> handleCacheInvocation(CacheInvocationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseObject(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        ex.getMessage(),
                        null
                ));
    }
    @ExceptionHandler(CacheRefreshException.class)
    public ResponseEntity<ResponseObject> handleCacheRefresh(CacheRefreshException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseObject(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        ex.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(AccessDenyException.class)
    public ResponseEntity<ResponseObject> handleAccessDenied(AccessDenyException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN) // 403 thay vì 500
                .body(new ResponseObject(
                        HttpStatus.FORBIDDEN.value(),
                        ex.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ResponseObject> handlePermissionDenied(PermissionDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN) // 403
                .body(new ResponseObject(
                        HttpStatus.FORBIDDEN.value(),
                        ex.getMessage(),
                        null
                ));
    }

}
