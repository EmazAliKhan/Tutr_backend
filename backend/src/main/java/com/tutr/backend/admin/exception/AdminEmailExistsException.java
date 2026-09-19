package com.tutr.backend.admin.exception;

public class AdminEmailExistsException extends RuntimeException {
    public AdminEmailExistsException(String message) {
        super(message);
    }
}