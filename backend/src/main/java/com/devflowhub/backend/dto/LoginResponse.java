package com.devflowhub.backend.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthenticatedCollaboratorResponse collaborator
) {
}
