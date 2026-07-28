package com.devflowhub.backend.exception;

public class ProjectAccessDeniedException extends RuntimeException {

    public ProjectAccessDeniedException() {
        this("You do not have permission to perform this operation.");
    }

    public ProjectAccessDeniedException(String message) {
        super(message);
    }
}
