package com.devflowhub.backend.service;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.dto.AddProjectMemberRequest;
import com.devflowhub.backend.dto.ProjectMemberResponse;
import com.devflowhub.backend.dto.UpdateProjectMemberRequest;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.entity.Project;
import com.devflowhub.backend.entity.ProjectMembership;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ProjectAccessDeniedException;
import com.devflowhub.backend.exception.ResourceConflictException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.ProjectMembershipRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import com.devflowhub.backend.security.ProjectAccessService;
import com.devflowhub.backend.security.ProjectPermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ProjectMembershipService {

    private static final String MEMBER_NOT_FOUND_MESSAGE =
            "Project member not found.";

    private static final String OWNER_OPERATION_MESSAGE =
            "Project owners must be managed through the dedicated ownership transfer operation.";

    private final ProjectMembershipRepository projectMembershipRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService projectAccessService;

    public ProjectMembershipService(
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

    public List<ProjectMemberResponse> findAll(Long projectId) {
        projectAccessService.requirePermission(
                projectId,
                ProjectPermission.VIEW_PROJECT
        );

        List<ProjectMembership> memberships =
                projectMembershipRepository
                        .findByProjectIdAndStatusOrderByCreatedAtAsc(
                                projectId,
                                ProjectMembershipStatus.ACTIVE
                        );

        Map<Long, Collaborator> collaborators =
                loadCollaborators(memberships);

        return memberships.stream()
                .map(membership -> toResponse(
                        membership,
                        collaborators.get(
                                membership.getCollaboratorId()
                        )
                ))
                .toList();
    }

    @Transactional
    public ProjectMemberResponse add(
            Long projectId,
            AddProjectMemberRequest request
    ) {
        ProjectMembership actor =
                projectAccessService.requirePermission(
                        projectId,
                        ProjectPermission.MANAGE_PROJECT_MEMBERS
                );

        requireSupportedManagedRole(request.role());
        requireCanAssignRole(actor.getRole(), request.role());

        Collaborator collaborator = getActiveCollaboratorRequired(
                request.collaboratorId()
        );

        ProjectMembership membership =
                projectMembershipRepository
                        .findByProjectIdAndCollaboratorId(
                                projectId,
                                request.collaboratorId()
                        )
                        .map(existing -> reactivate(
                                existing,
                                request.role()
                        ))
                        .orElseGet(() -> newMembership(
                                projectId,
                                request.collaboratorId(),
                                request.role()
                        ));

        ProjectMembership saved =
                projectMembershipRepository.saveAndFlush(
                        membership
                );

        return toResponse(saved, collaborator);
    }

    @Transactional
    public ProjectMemberResponse updateRole(
            Long projectId,
            Long collaboratorId,
            UpdateProjectMemberRequest request
    ) {
        ProjectMembership actor =
                projectAccessService.requirePermission(
                        projectId,
                        ProjectPermission.MANAGE_PROJECT_MEMBERS
                );

        ProjectMembership target = getActiveMembershipRequired(
                projectId,
                collaboratorId
        );

        requireMatchingVersion(target, request.version());
        requireSupportedManagedRole(target.getRole());
        requireSupportedManagedRole(request.role());
        requireCanManageCurrentRole(
                actor.getRole(),
                target.getRole()
        );
        requireCanAssignRole(actor.getRole(), request.role());
        requireManagerReferenceRemainsValid(
                projectId,
                collaboratorId,
                request.role()
        );

        Collaborator collaborator =
                getActiveCollaboratorRequired(collaboratorId);

        if (target.getRole() == request.role()) {
            return toResponse(target, collaborator);
        }

        target.setRole(request.role());

        ProjectMembership saved =
                projectMembershipRepository.saveAndFlush(target);

        return toResponse(saved, collaborator);
    }

    @Transactional
    public void remove(
            Long projectId,
            Long collaboratorId
    ) {
        ProjectMembership actor =
                projectAccessService.requirePermission(
                        projectId,
                        ProjectPermission.MANAGE_PROJECT_MEMBERS
                );

        ProjectMembership target = getActiveMembershipRequired(
                projectId,
                collaboratorId
        );

        requireSupportedManagedRole(target.getRole());
        requireCanManageCurrentRole(
                actor.getRole(),
                target.getRole()
        );
        requireNotCurrentProjectManager(
                projectId,
                collaboratorId
        );

        target.setStatus(ProjectMembershipStatus.INACTIVE);
        projectMembershipRepository.saveAndFlush(target);
    }

    private ProjectMembership reactivate(
            ProjectMembership existing,
            ProjectMembershipRole requestedRole
    ) {
        if (existing.getStatus() == ProjectMembershipStatus.ACTIVE) {
            throw new ResourceConflictException(
                    "The collaborator is already an active member of this project."
            );
        }

        requireSupportedManagedRole(existing.getRole());

        existing.setRole(requestedRole);
        existing.setStatus(ProjectMembershipStatus.ACTIVE);
        return existing;
    }

    private ProjectMembership newMembership(
            Long projectId,
            Long collaboratorId,
            ProjectMembershipRole role
    ) {
        ProjectMembership membership = new ProjectMembership();
        membership.setProjectId(projectId);
        membership.setCollaboratorId(collaboratorId);
        membership.setRole(role);
        membership.setStatus(ProjectMembershipStatus.ACTIVE);
        return membership;
    }

    private ProjectMembership getActiveMembershipRequired(
            Long projectId,
            Long collaboratorId
    ) {
        return projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        projectId,
                        collaboratorId,
                        ProjectMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        MEMBER_NOT_FOUND_MESSAGE
                ));
    }

    private Collaborator getActiveCollaboratorRequired(
            Long collaboratorId
    ) {
        if (collaboratorId == null) {
            throw new InvalidOperationException(
                    "Collaborator ID is required."
            );
        }

        Collaborator collaborator = collaboratorRepository
                .findById(collaboratorId)
                .orElseThrow(() -> new InvalidOperationException(
                        "The selected collaborator does not exist."
                ));

        if (!Boolean.TRUE.equals(collaborator.getActive())) {
            throw new InvalidOperationException(
                    "Only active collaborators can belong to a project."
            );
        }

        return collaborator;
    }

    private void requireSupportedManagedRole(
            ProjectMembershipRole role
    ) {
        if (role == null) {
            throw new InvalidOperationException(
                    "Membership role is required."
            );
        }

        if (role == ProjectMembershipRole.OWNER) {
            throw new InvalidOperationException(
                    OWNER_OPERATION_MESSAGE
            );
        }
    }

    private void requireCanManageCurrentRole(
            ProjectMembershipRole actorRole,
            ProjectMembershipRole targetRole
    ) {
        if (actorRole == ProjectMembershipRole.OWNER) {
            return;
        }

        if (
            actorRole == ProjectMembershipRole.MANAGER &&
            isContributorOrViewer(targetRole)
        ) {
            return;
        }

        throw new ProjectAccessDeniedException();
    }

    private void requireCanAssignRole(
            ProjectMembershipRole actorRole,
            ProjectMembershipRole requestedRole
    ) {
        if (actorRole == ProjectMembershipRole.OWNER) {
            return;
        }

        if (
            actorRole == ProjectMembershipRole.MANAGER &&
            isContributorOrViewer(requestedRole)
        ) {
            return;
        }

        throw new ProjectAccessDeniedException();
    }

    private boolean isContributorOrViewer(
            ProjectMembershipRole role
    ) {
        return role == ProjectMembershipRole.CONTRIBUTOR ||
                role == ProjectMembershipRole.VIEWER;
    }

    private void requireMatchingVersion(
            ProjectMembership membership,
            Long requestedVersion
    ) {
        if (requestedVersion == null) {
            throw new InvalidOperationException(
                    "Membership version is required."
            );
        }

        if (!Objects.equals(
                membership.getVersion(),
                requestedVersion
        )) {
            throw new ResourceConflictException(
                    "The project membership was modified by another request. Refresh and try again."
            );
        }
    }

    private void requireManagerReferenceRemainsValid(
            Long projectId,
            Long collaboratorId,
            ProjectMembershipRole requestedRole
    ) {
        if (
            requestedRole == ProjectMembershipRole.MANAGER ||
            requestedRole == ProjectMembershipRole.OWNER
        ) {
            return;
        }

        requireNotCurrentProjectManager(
                projectId,
                collaboratorId
        );
    }

    private void requireNotCurrentProjectManager(
            Long projectId,
            Long collaboratorId
    ) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found."
                ));

        if (Objects.equals(
                project.getManagerId(),
                collaboratorId
        )) {
            throw new InvalidOperationException(
                    "Select another project manager before removing or demoting this member."
            );
        }
    }

    private Map<Long, Collaborator> loadCollaborators(
            List<ProjectMembership> memberships
    ) {
        List<Long> collaboratorIds = memberships.stream()
                .map(ProjectMembership::getCollaboratorId)
                .distinct()
                .toList();

        Map<Long, Collaborator> collaborators =
                new LinkedHashMap<>();

        collaboratorRepository.findAllById(collaboratorIds)
                .forEach(collaborator -> collaborators.put(
                        collaborator.getId(),
                        collaborator
                ));

        if (collaborators.size() != collaboratorIds.size()) {
            throw new IllegalStateException(
                    "A project membership references a missing collaborator."
            );
        }

        return collaborators;
    }

    private ProjectMemberResponse toResponse(
            ProjectMembership membership,
            Collaborator collaborator
    ) {
        if (collaborator == null) {
            throw new IllegalStateException(
                    "A project membership references a missing collaborator."
            );
        }

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
