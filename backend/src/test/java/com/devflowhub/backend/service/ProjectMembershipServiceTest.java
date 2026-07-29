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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMembershipServiceTest {

    @Mock
    private ProjectMembershipRepository projectMembershipRepository;

    @Mock
    private CollaboratorRepository collaboratorRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    private ProjectMembershipService projectMembershipService;

    @BeforeEach
    void setUp() {
        projectMembershipService = new ProjectMembershipService(
                projectMembershipRepository,
                collaboratorRepository,
                projectRepository,
                projectAccessService
        );
    }

    @Test
    void findAllReturnsActiveMembersWithCollaboratorDetails() {
        ProjectMembership owner = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );
        ProjectMembership contributor = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.CONTRIBUTOR,
                ProjectMembershipStatus.ACTIVE,
                2L
        );

        Collaborator ownerCollaborator = collaborator(
                7L,
                "Owner User",
                "Product Manager",
                true
        );
        Collaborator contributorCollaborator = collaborator(
                8L,
                "Contributor User",
                "Backend Developer",
                true
        );

        when(projectMembershipRepository
                .findByProjectIdAndStatusOrderByCreatedAtAsc(
                        11L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(List.of(owner, contributor));
        when(collaboratorRepository.findAllById(
                List.of(7L, 8L)
        )).thenReturn(List.of(
                ownerCollaborator,
                contributorCollaborator
        ));

        List<ProjectMemberResponse> result =
                projectMembershipService.findAll(11L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).collaboratorName())
                .isEqualTo("Owner User");
        assertThat(result.get(0).role())
                .isEqualTo(ProjectMembershipRole.OWNER);
        assertThat(result.get(1).collaboratorRole())
                .isEqualTo("Backend Developer");
        assertThat(result.get(1).version())
                .isEqualTo(2L);

        InOrder order = inOrder(
                projectAccessService,
                projectMembershipRepository,
                collaboratorRepository
        );
        order.verify(projectAccessService)
                .requirePermission(
                        11L,
                        ProjectPermission.VIEW_PROJECT
                );
        order.verify(projectMembershipRepository)
                .findByProjectIdAndStatusOrderByCreatedAtAsc(
                        11L,
                        ProjectMembershipStatus.ACTIVE
                );
        order.verify(collaboratorRepository)
                .findAllById(List.of(7L, 8L));
    }

    @Test
    void findAllDoesNotQueryMembersWhenProjectIsHidden() {
        doThrow(new ResourceNotFoundException(
                "Project not found."
        )).when(projectAccessService)
                .requirePermission(
                        11L,
                        ProjectPermission.VIEW_PROJECT
                );

        assertThatThrownBy(() ->
                projectMembershipService.findAll(11L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found.");

        verifyNoInteractions(
                projectMembershipRepository,
                collaboratorRepository,
                projectRepository
        );
    }

    @Test
    void ownerAddsNewManager() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );
        Collaborator collaborator = collaborator(
                8L,
                "Manager User",
                "Team Lead",
                true
        );

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);
        when(collaboratorRepository.findById(8L))
                .thenReturn(Optional.of(collaborator));
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorId(
                        11L,
                        8L
                ))
                .thenReturn(Optional.empty());
        when(projectMembershipRepository.saveAndFlush(
                any(ProjectMembership.class)
        )).thenAnswer(invocation -> {
            ProjectMembership saved = invocation.getArgument(0);
            saved.setId(20L);
            saved.setVersion(0L);
            return saved;
        });

        ProjectMemberResponse result =
                projectMembershipService.add(
                        11L,
                        new AddProjectMemberRequest(
                                8L,
                                ProjectMembershipRole.MANAGER
                        )
                );

        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.projectId()).isEqualTo(11L);
        assertThat(result.collaboratorId()).isEqualTo(8L);
        assertThat(result.role())
                .isEqualTo(ProjectMembershipRole.MANAGER);
        assertThat(result.status())
                .isEqualTo(ProjectMembershipStatus.ACTIVE);

        ArgumentCaptor<ProjectMembership> captor =
                ArgumentCaptor.forClass(ProjectMembership.class);
        verify(projectMembershipRepository)
                .saveAndFlush(captor.capture());

        assertThat(captor.getValue().getProjectId())
                .isEqualTo(11L);
        assertThat(captor.getValue().getCollaboratorId())
                .isEqualTo(8L);
        assertThat(captor.getValue().getRole())
                .isEqualTo(ProjectMembershipRole.MANAGER);
    }

    @Test
    void managerAddsContributor() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.MANAGER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );
        Collaborator collaborator = collaborator(
                8L,
                "Contributor User",
                "Developer",
                true
        );

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);
        when(collaboratorRepository.findById(8L))
                .thenReturn(Optional.of(collaborator));
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorId(
                        11L,
                        8L
                ))
                .thenReturn(Optional.empty());
        when(projectMembershipRepository.saveAndFlush(
                any(ProjectMembership.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectMemberResponse result =
                projectMembershipService.add(
                        11L,
                        new AddProjectMemberRequest(
                                8L,
                                ProjectMembershipRole.CONTRIBUTOR
                        )
                );

        assertThat(result.role())
                .isEqualTo(ProjectMembershipRole.CONTRIBUTOR);
    }

    @Test
    void managerCannotAddAnotherManager() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.MANAGER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);

        assertThatThrownBy(() ->
                projectMembershipService.add(
                        11L,
                        new AddProjectMemberRequest(
                                8L,
                                ProjectMembershipRole.MANAGER
                        )
                )
        )
                .isInstanceOf(ProjectAccessDeniedException.class)
                .hasMessage(
                        "You do not have permission to perform this operation."
                );

        verifyNoInteractions(
                collaboratorRepository,
                projectMembershipRepository,
                projectRepository
        );
    }

    @Test
    void genericAddRejectsOwnerRole() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);

        assertThatThrownBy(() ->
                projectMembershipService.add(
                        11L,
                        new AddProjectMemberRequest(
                                8L,
                                ProjectMembershipRole.OWNER
                        )
                )
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "Project owners must be managed through the dedicated ownership transfer operation."
                );

        verifyNoInteractions(
                collaboratorRepository,
                projectMembershipRepository,
                projectRepository
        );
    }

    @Test
    void addRejectsInactiveCollaborator() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );
        Collaborator inactive = collaborator(
                8L,
                "Inactive User",
                "Developer",
                false
        );

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);
        when(collaboratorRepository.findById(8L))
                .thenReturn(Optional.of(inactive));

        assertThatThrownBy(() ->
                projectMembershipService.add(
                        11L,
                        new AddProjectMemberRequest(
                                8L,
                                ProjectMembershipRole.CONTRIBUTOR
                        )
                )
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "Only active collaborators can belong to a project."
                );

        verify(projectMembershipRepository, never())
                .saveAndFlush(any(ProjectMembership.class));
    }

    @Test
    void addRejectsExistingActiveMembershipAsConflict() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );
        ProjectMembership existing = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.CONTRIBUTOR,
                ProjectMembershipStatus.ACTIVE,
                1L
        );
        Collaborator collaborator = collaborator(
                8L,
                "Existing User",
                "Developer",
                true
        );

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);
        when(collaboratorRepository.findById(8L))
                .thenReturn(Optional.of(collaborator));
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorId(
                        11L,
                        8L
                ))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                projectMembershipService.add(
                        11L,
                        new AddProjectMemberRequest(
                                8L,
                                ProjectMembershipRole.VIEWER
                        )
                )
        )
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage(
                        "The collaborator is already an active member of this project."
                );

        verify(projectMembershipRepository, never())
                .saveAndFlush(any(ProjectMembership.class));
    }

    @Test
    void addReactivatesInactiveMembership() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );
        ProjectMembership inactive = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.VIEWER,
                ProjectMembershipStatus.INACTIVE,
                4L
        );
        Collaborator collaborator = collaborator(
                8L,
                "Returning User",
                "Developer",
                true
        );

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);
        when(collaboratorRepository.findById(8L))
                .thenReturn(Optional.of(collaborator));
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorId(
                        11L,
                        8L
                ))
                .thenReturn(Optional.of(inactive));
        when(projectMembershipRepository.saveAndFlush(inactive))
                .thenReturn(inactive);

        ProjectMemberResponse result =
                projectMembershipService.add(
                        11L,
                        new AddProjectMemberRequest(
                                8L,
                                ProjectMembershipRole.CONTRIBUTOR
                        )
                );

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.role())
                .isEqualTo(ProjectMembershipRole.CONTRIBUTOR);
        assertThat(result.status())
                .isEqualTo(ProjectMembershipStatus.ACTIVE);
        verify(projectMembershipRepository)
                .saveAndFlush(inactive);
    }

    @Test
    void updateRoleRejectsStaleVersion() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );
        ProjectMembership target = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.CONTRIBUTOR,
                ProjectMembershipStatus.ACTIVE,
                3L
        );

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(target));

        assertThatThrownBy(() ->
                projectMembershipService.updateRole(
                        11L,
                        8L,
                        new UpdateProjectMemberRequest(
                                ProjectMembershipRole.VIEWER,
                                2L
                        )
                )
        )
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage(
                        "The project membership was modified by another request. Refresh and try again."
                );

        verifyNoInteractions(
                collaboratorRepository,
                projectRepository
        );
        verify(projectMembershipRepository, never())
                .saveAndFlush(any(ProjectMembership.class));
    }

    @Test
    void managerCanChangeContributorToViewer() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.MANAGER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );
        ProjectMembership target = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.CONTRIBUTOR,
                ProjectMembershipStatus.ACTIVE,
                3L
        );
        Collaborator collaborator = collaborator(
                8L,
                "Contributor User",
                "Developer",
                true
        );
        Project project = project(11L, 7L);

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(target));
        when(projectRepository.findById(11L))
                .thenReturn(Optional.of(project));
        when(collaboratorRepository.findById(8L))
                .thenReturn(Optional.of(collaborator));
        when(projectMembershipRepository.saveAndFlush(target))
                .thenReturn(target);

        ProjectMemberResponse result =
                projectMembershipService.updateRole(
                        11L,
                        8L,
                        new UpdateProjectMemberRequest(
                                ProjectMembershipRole.VIEWER,
                                3L
                        )
                );

        assertThat(result.role())
                .isEqualTo(ProjectMembershipRole.VIEWER);
        verify(projectMembershipRepository)
                .saveAndFlush(target);
    }

    @Test
    void managerCannotModifyAnotherManager() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.MANAGER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );
        ProjectMembership target = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.MANAGER,
                ProjectMembershipStatus.ACTIVE,
                1L
        );

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(target));

        assertThatThrownBy(() ->
                projectMembershipService.updateRole(
                        11L,
                        8L,
                        new UpdateProjectMemberRequest(
                                ProjectMembershipRole.CONTRIBUTOR,
                                1L
                        )
                )
        ).isInstanceOf(ProjectAccessDeniedException.class);

        verifyNoInteractions(
                collaboratorRepository,
                projectRepository
        );
    }

    @Test
    void genericUpdateRejectsOwnerMembership() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );
        ProjectMembership target = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE,
                1L
        );

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(target));

        assertThatThrownBy(() ->
                projectMembershipService.updateRole(
                        11L,
                        8L,
                        new UpdateProjectMemberRequest(
                                ProjectMembershipRole.MANAGER,
                                1L
                        )
                )
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "Project owners must be managed through the dedicated ownership transfer operation."
                );

        verifyNoInteractions(
                collaboratorRepository,
                projectRepository
        );
    }

    @Test
    void updateRolePreventsDemotingCurrentProjectManager() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );
        ProjectMembership target = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.MANAGER,
                ProjectMembershipStatus.ACTIVE,
                1L
        );
        Project project = project(11L, 8L);

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(target));
        when(projectRepository.findById(11L))
                .thenReturn(Optional.of(project));

        assertThatThrownBy(() ->
                projectMembershipService.updateRole(
                        11L,
                        8L,
                        new UpdateProjectMemberRequest(
                                ProjectMembershipRole.CONTRIBUTOR,
                                1L
                        )
                )
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "Select another project manager before removing or demoting this member."
                );

        verifyNoInteractions(collaboratorRepository);
        verify(projectMembershipRepository, never())
                .saveAndFlush(any(ProjectMembership.class));
    }

    @Test
    void updateRoleReturnsExistingMembershipForNoOp() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );
        ProjectMembership target = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.MANAGER,
                ProjectMembershipStatus.ACTIVE,
                1L
        );
        Collaborator collaborator = collaborator(
                8L,
                "Manager User",
                "Team Lead",
                true
        );

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(target));
        when(collaboratorRepository.findById(8L))
                .thenReturn(Optional.of(collaborator));

        ProjectMemberResponse result =
                projectMembershipService.updateRole(
                        11L,
                        8L,
                        new UpdateProjectMemberRequest(
                                ProjectMembershipRole.MANAGER,
                                1L
                        )
                );

        assertThat(result.role())
                .isEqualTo(ProjectMembershipRole.MANAGER);
        assertThat(result.version()).isEqualTo(1L);
        verify(projectMembershipRepository, never())
                .saveAndFlush(any(ProjectMembership.class));
        verifyNoInteractions(projectRepository);
    }

    @Test
    void ownerRemovesContributorBySettingMembershipInactive() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );
        ProjectMembership target = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.CONTRIBUTOR,
                ProjectMembershipStatus.ACTIVE,
                1L
        );
        Project project = project(11L, 7L);

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(target));
        when(projectRepository.findById(11L))
                .thenReturn(Optional.of(project));
        when(projectMembershipRepository.saveAndFlush(target))
                .thenReturn(target);

        projectMembershipService.remove(11L, 8L);

        assertThat(target.getStatus())
                .isEqualTo(ProjectMembershipStatus.INACTIVE);
        verify(projectMembershipRepository)
                .saveAndFlush(target);
    }

    @Test
    void removePreventsRemovingCurrentProjectManager() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );
        ProjectMembership target = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.MANAGER,
                ProjectMembershipStatus.ACTIVE,
                1L
        );
        Project project = project(11L, 8L);

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(target));
        when(projectRepository.findById(11L))
                .thenReturn(Optional.of(project));

        assertThatThrownBy(() ->
                projectMembershipService.remove(11L, 8L)
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "Select another project manager before removing or demoting this member."
                );

        verify(projectMembershipRepository, never())
                .saveAndFlush(any(ProjectMembership.class));
    }

    @Test
    void managerCannotRemoveAnotherManager() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.MANAGER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );
        ProjectMembership target = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.MANAGER,
                ProjectMembershipStatus.ACTIVE,
                1L
        );

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(target));

        assertThatThrownBy(() ->
                projectMembershipService.remove(11L, 8L)
        ).isInstanceOf(ProjectAccessDeniedException.class);

        verifyNoInteractions(projectRepository);
        verify(projectMembershipRepository, never())
                .saveAndFlush(any(ProjectMembership.class));
    }

    @Test
    void updateRoleHidesMissingActiveMemberAsNotFound() {
        ProjectMembership actor = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE,
                0L
        );

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.MANAGE_PROJECT_MEMBERS
        )).thenReturn(actor);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                projectMembershipService.updateRole(
                        11L,
                        8L,
                        new UpdateProjectMemberRequest(
                                ProjectMembershipRole.VIEWER,
                                0L
                        )
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project member not found.");
    }

    private ProjectMembership membership(
            Long id,
            Long projectId,
            Long collaboratorId,
            ProjectMembershipRole role,
            ProjectMembershipStatus status,
            Long version
    ) {
        ProjectMembership membership = new ProjectMembership();
        membership.setId(id);
        membership.setProjectId(projectId);
        membership.setCollaboratorId(collaboratorId);
        membership.setRole(role);
        membership.setStatus(status);
        membership.setVersion(version);
        membership.setCreatedAt(
                LocalDateTime.of(2026, 7, 29, 10, 0)
        );
        membership.setUpdatedAt(
                LocalDateTime.of(2026, 7, 29, 10, 0)
        );
        return membership;
    }

    private Collaborator collaborator(
            Long id,
            String name,
            String role,
            boolean active
    ) {
        Collaborator collaborator = new Collaborator();
        collaborator.setId(id);
        collaborator.setName(name);
        collaborator.setRole(role);
        collaborator.setActive(active);
        return collaborator;
    }

    private Project project(
            Long id,
            Long managerId
    ) {
        Project project = new Project();
        project.setId(id);
        project.setName("Project");
        project.setStatus("PLANNED");
        project.setManagerId(managerId);
        return project;
    }
}
