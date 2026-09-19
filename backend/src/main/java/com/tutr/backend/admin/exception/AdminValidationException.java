package com.tutr.backend.admin.exception;

public class AdminValidationException extends RuntimeException {
    public AdminValidationException(String message) {
        super(message);
    }
}