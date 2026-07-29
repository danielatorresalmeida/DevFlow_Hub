package com.devflowhub.backend.service;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.dto.ProjectMemberResponse;
import com.devflowhub.backend.dto.ProjectOwnershipTransferResponse;
import com.devflowhub.backend.dto.TransferProjectOwnershipRequest;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.entity.Project;
import com.devflowhub.backend.entity.ProjectMembership;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceConflictException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.ProjectMembershipRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import com.devflowhub.backend.security.ProjectAccessService;
import com.devflowhub.backend.security.ProjectPermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ProjectOwnershipService {

    private static final String PROJECT_NOT_FOUND_MESSAGE =
            "Project not found.";

    private static final String ACTIVE_MEMBER_REQUIRED_MESSAGE =
            "The new owner must be an active member of the project.";

    private final ProjectMembershipRepository projectMembershipRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService projectAccessService;

    public ProjectOwnershipService(
            ProjectMembershipRepository projectMembershipRepository,
            CollaboratorRepository collaboratorRepository,
            ProjectRepository projectRepository,
            ProjectAccessService projectAccessService
    ) {
        this.projectMembershipRepository = projectMembershipRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.projectRepository = projectRepository;
        this.projectAccessService = projectAccessService;
    }

    @Transactional
    public ProjectOwnershipTransferResponse transfer(
            Long projectId,
            TransferProjectOwnershipRequest request
    ) {
        ProjectMembership currentOwner =
                projectAccessService.requirePermission(
                        projectId,
                        ProjectPermission.TRANSFER_PROJECT_OWNERSHIP
                );

        Long newOwnerCollaboratorId =
                request.newOwnerCollaboratorId();

        if (Objects.equals(
                currentOwner.getCollaboratorId(),
                newOwnerCollaboratorId
        )) {
            throw new InvalidOperationException(
                    "Ownership cannot be transferred to the current owner."
            );
        }

        requireSingleActiveOwner(projectId);
        requireMatchingVersion(
                currentOwner,
                request.currentOwnerMembershipVersion(),
                "Current owner"
        );

        ProjectMembership newOwner = projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        projectId,
                        newOwnerCollaboratorId,
                        ProjectMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new InvalidOperationException(
                        ACTIVE_MEMBER_REQUIRED_MESSAGE
                ));

        requireMatchingVersion(
                newOwner,
                request.newOwnerMembershipVersion(),
                "New owner"
        );

        if (newOwner.getRole() == ProjectMembershipRole.OWNER) {
            throw new ResourceConflictException(
                    "The selected collaborator already owns this project."
            );
        }

        Collaborator currentOwnerCollaborator =
                collaboratorRepository
                        .findById(currentOwner.getCollaboratorId())
                        .orElseThrow(() -> new IllegalStateException(
                                "The current owner membership references a missing collaborator."
                        ));

        Collaborator newOwnerCollaborator =
                collaboratorRepository
                        .findById(newOwnerCollaboratorId)
                        .orElseThrow(() -> new InvalidOperationException(
                                "The selected collaborator does not exist."
                        ));

        if (!Boolean.TRUE.equals(newOwnerCollaborator.getActive())) {
            throw new InvalidOperationException(
                    "The new owner collaborator must be active."
            );
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        PROJECT_NOT_FOUND_MESSAGE
                ));

        currentOwner.setRole(ProjectMembershipRole.MANAGER);
        newOwner.setRole(ProjectMembershipRole.OWNER);
        project.setManagerId(newOwnerCollaboratorId);

        projectMembershipRepository.saveAllAndFlush(
                List.of(currentOwner, newOwner)
        );
        Project savedProject = projectRepository.saveAndFlush(project);

        return new ProjectOwnershipTransferResponse(
                projectId,
                savedProject.getManagerId(),
                toResponse(currentOwner, currentOwnerCollaborator),
                toResponse(newOwner, newOwnerCollaborator)
        );
    }

    private void requireSingleActiveOwner(Long projectId) {
        long ownerCount = projectMembershipRepository
                .countByProjectIdAndRoleAndStatus(
                        projectId,
                        ProjectMembershipRole.OWNER,
                        ProjectMembershipStatus.ACTIVE
                );

        if (ownerCount != 1L) {
            throw new ResourceConflictException(
                    "Project ownership is inconsistent. Refresh and contact an administrator."
            );
        }
    }

    private void requireMatchingVersion(
            ProjectMembership membership,
            Long requestedVersion,
            String label
    ) {
        if (!Objects.equals(
                membership.getVersion(),
                requestedVersion
        )) {
            throw new ResourceConflictException(
                    label + " membership was modified by another request. Refresh and try again."
            );
        }
    }

    private ProjectMemberResponse toResponse(
            ProjectMembership membership,
            Collaborator collaborator
    ) {
        return new ProjectMemberResponse(
                membership.getId(),
                membership.getProjectId(),
                membership.getCollaboratorId(),
                collaborator.getName(),
                collaborator.getRole(),
                membership.getRole(),
                membership.getStatus(),
                membership.getVersion(),
                membership.getCreatedAt(),
                membership.getUpdatedAt()
        );
    }
}
