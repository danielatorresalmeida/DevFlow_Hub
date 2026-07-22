package com.devflowhub.backend.dto;

public record AuthenticatedCollaboratorResponse(
        Long id,
        String name,
        String email,
        String role,
        boolean active
) {
}
