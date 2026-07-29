package com.devflowhub.backend.dto;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import jakarta.validation.constraints.NotNull;

public record AddProjectMemberRequest(
        @NotNull(message = "Collaborator ID is required.")
        Long collaboratorId,

        @NotNull(message = "Membership role is required.")
        ProjectMembershipRole role
) {
}
