package com.example.demologin.exception.exceptions;

public class AccessDenyException extends RuntimeException {
    public AccessDenyException(String message) {
        super(message);
    }
}
