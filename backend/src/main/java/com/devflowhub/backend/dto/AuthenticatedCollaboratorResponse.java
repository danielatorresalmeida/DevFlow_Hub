package com.devflowhub.backend.dto;

import com.devflowhub.backend.domain.SystemRole;

public record AuthenticatedCollaboratorResponse(
        Long id,
        String name,
        String email,
        String role,
        SystemRole systemRole,
        boolean active
) {
}
