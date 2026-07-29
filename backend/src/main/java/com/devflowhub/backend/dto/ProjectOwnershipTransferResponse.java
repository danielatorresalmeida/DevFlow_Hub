package com.devflowhub.backend.dto;

public record ProjectOwnershipTransferResponse(
        Long projectId,
        Long managerId,
        ProjectMemberResponse previousOwner,
        ProjectMemberResponse newOwner
) {
}
