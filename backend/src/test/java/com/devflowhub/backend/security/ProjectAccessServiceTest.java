package com.devflowhub.backend.security;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.entity.ProjectMembership;
import com.devflowhub.backend.exception.ProjectAccessDeniedException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.ProjectMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAccessServiceTest {

    @Mock
    private CurrentCollaboratorResolver currentCollaboratorResolver;

    @Mock
    private ProjectMembershipRepository projectMembershipRepository;

    private ProjectAccessService projectAccessService;

    @BeforeEach
    void setUp() {
        projectAccessService = new ProjectAccessService(
                currentCollaboratorResolver,
                projectMembershipRepository
        );
    }

    @Test
    void requirePermissionReturnsAuthorizedActiveMembership() {
        ProjectMembership membership = membership(
                ProjectMembershipRole.CONTRIBUTOR,
                ProjectMembershipStatus.ACTIVE
        );

        when(currentCollaboratorResolver.getRequiredId())
                .thenReturn(7L);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        7L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(membership));

        ProjectMembership result = projectAccessService
                .requirePermission(
                        11L,
                        ProjectPermission.CONTRIBUTE_TO_PROJECT
                );

        assertThat(result).isSameAs(membership);
    }

    @Test
    void requirePermissionHidesMissingMembershipAsNotFound() {
        when(currentCollaboratorResolver.getRequiredId())
                .thenReturn(7L);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        7L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectAccessService
                .requirePermission(
                        11L,
                        ProjectPermission.VIEW_PROJECT
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found.");
    }

    @Test
    void requirePermissionOnlyQueriesActiveMemberships() {
        when(currentCollaboratorResolver.getRequiredId())
                .thenReturn(7L);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        7L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectAccessService
                .requirePermission(
                        11L,
                        ProjectPermission.VIEW_PROJECT
                ))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectMembershipRepository)
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        7L,
                        ProjectMembershipStatus.ACTIVE
                );
    }

    @Test
    void requirePermissionRejectsInsufficientRoleAsForbidden() {
        ProjectMembership membership = membership(
                ProjectMembershipRole.VIEWER,
                ProjectMembershipStatus.ACTIVE
        );

        when(currentCollaboratorResolver.getRequiredId())
                .thenReturn(7L);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        7L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> projectAccessService
                .requirePermission(
                        11L,
                        ProjectPermission.MANAGE_PROJECT
                ))
                .isInstanceOf(ProjectAccessDeniedException.class)
                .hasMessage(
                        "You do not have permission to perform this operation."
                );
    }

    @Test
    void requirePermissionHidesNullProjectIdAsNotFound() {
        assertThatThrownBy(() -> projectAccessService
                .requirePermission(
                        null,
                        ProjectPermission.VIEW_PROJECT
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found.");
    }

    private ProjectMembership membership(
            ProjectMembershipRole role,
            ProjectMembershipStatus status
    ) {
        ProjectMembership membership = new ProjectMembership();
        membership.setId(1L);
        membership.setProjectId(11L);
        membership.setCollaboratorId(7L);
        membership.setRole(role);
        membership.setStatus(status);
        return membership;
    }
}
