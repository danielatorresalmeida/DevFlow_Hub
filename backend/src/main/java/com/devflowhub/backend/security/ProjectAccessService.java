package com.devflowhub.backend.security;

import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.entity.ProjectMembership;
import com.devflowhub.backend.exception.ProjectAccessDeniedException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.ProjectMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ProjectAccessService {

    private static final String HIDDEN_PROJECT_MESSAGE =
            "Project not found.";

    private final CurrentCollaboratorResolver currentCollaboratorResolver;
    private final ProjectMembershipRepository projectMembershipRepository;

    public ProjectAccessService(
            CurrentCollaboratorResolver currentCollaboratorResolver,
            ProjectMembershipRepository projectMembershipRepository
    ) {
        this.currentCollaboratorResolver = currentCollaboratorResolver;
        this.projectMembershipRepository = projectMembershipRepository;
    }

    public ProjectMembership requirePermission(
            Long projectId,
            ProjectPermission permission
    ) {
        if (projectId == null) {
            throw new ResourceNotFoundException(HIDDEN_PROJECT_MESSAGE);
        }

        Objects.requireNonNull(
                permission,
                "Project permission is required."
        );

        Long collaboratorId = currentCollaboratorResolver.getRequiredId();

        ProjectMembership membership = projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        projectId,
                        collaboratorId,
                        ProjectMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        HIDDEN_PROJECT_MESSAGE
                ));

        if (!permission.isGrantedTo(membership.getRole())) {
            throw new ProjectAccessDeniedException();
        }

        return membership;
    }
}
