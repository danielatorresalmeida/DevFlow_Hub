package com.devflowhub.backend.exception;

public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        this("Invalid email or password.");
    }

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
