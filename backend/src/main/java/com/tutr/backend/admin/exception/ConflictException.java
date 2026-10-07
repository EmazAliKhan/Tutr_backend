package com.tutr.backend.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // 409
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}