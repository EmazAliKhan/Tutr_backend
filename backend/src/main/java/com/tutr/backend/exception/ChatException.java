package com.tutr.backend.exception;

public class ChatException extends RuntimeException {

    private final String errorCode;

    public ChatException(String message) {
        super(message);
        this.errorCode = "CHAT_ERROR";
    }

    public ChatException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}