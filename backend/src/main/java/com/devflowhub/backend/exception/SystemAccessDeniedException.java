package com.devflowhub.backend.exception;

public class SystemAccessDeniedException extends RuntimeException {

    public SystemAccessDeniedException() {
        this("Administrator access is required.");
    }

    public SystemAccessDeniedException(String message) {
        super(message);
    }
}
