package com.devflowhub.backend.dto;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateProjectMemberRequest(
        @NotNull(message = "Membership role is required.")
        ProjectMembershipRole role,

        @NotNull(message = "Membership version is required.")
        @PositiveOrZero(message = "Membership version cannot be negative.")
        Long version
) {
}
