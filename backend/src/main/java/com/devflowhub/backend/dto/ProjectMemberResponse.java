package com.devflowhub.backend.dto;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import com.devflowhub.backend.domain.ProjectMembershipStatus;

import java.time.LocalDateTime;

public record ProjectMemberResponse(
        Long id,
        Long projectId,
        Long collaboratorId,
        String collaboratorName,
        String collaboratorRole,
        ProjectMembershipRole role,
        ProjectMembershipStatus status,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
