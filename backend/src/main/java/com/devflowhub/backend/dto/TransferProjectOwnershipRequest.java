package com.devflowhub.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record TransferProjectOwnershipRequest(
        @NotNull(message = "New owner collaborator ID is required.")
        Long newOwnerCollaboratorId,

        @NotNull(message = "Current owner membership version is required.")
        @PositiveOrZero(message = "Current owner membership version cannot be negative.")
        Long currentOwnerMembershipVersion,

        @NotNull(message = "New owner membership version is required.")
        @PositiveOrZero(message = "New owner membership version cannot be negative.")
        Long newOwnerMembershipVersion
) {
}
