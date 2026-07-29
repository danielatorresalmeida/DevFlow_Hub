package com.devflowhub.backend.service;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.entity.Project;
import com.devflowhub.backend.entity.ProjectMembership;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.ProjectMembershipRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import com.devflowhub.backend.security.CurrentCollaboratorResolver;
import com.devflowhub.backend.security.ProjectAccessService;
import com.devflowhub.backend.security.ProjectPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CollaboratorRepository collaboratorRepository;

    @Mock
    private ProjectMembershipRepository projectMembershipRepository;

    @Mock
    private CurrentCollaboratorResolver currentCollaboratorResolver;

    @Mock
    private ProjectAccessService projectAccessService;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(
                projectRepository,
                collaboratorRepository,
                projectMembershipRepository,
                currentCollaboratorResolver,
                projectAccessService
        );
    }

    @Test
    void findAllReturnsOnlyProjectsAccessibleToCurrentCollaborator() {
        Project alpha = project(1L, "Alpha");
        Project beta = project(2L, "Beta");

        when(currentCollaboratorResolver.getRequiredId())
                .thenReturn(7L);

        when(projectRepository
                .findAccessibleByCollaboratorIdAndStatus(
                        7L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(List.of(alpha, beta));

        assertThat(projectService.findAll())
                .containsExactly(alpha, beta);
    }

    @Test
    void findByIdRequiresViewPermissionBeforeRepositoryLookup() {
        Project project = project(11L, "Website");

        when(projectRepository.findById(11L))
                .thenReturn(Optional.of(project));

        assertThat(projectService.findById(11L))
                .contains(project);

        InOrder order = inOrder(
                projectAccessService,
                projectRepository
        );

        order.verify(projectAccessService)
                .requirePermission(
                        11L,
                        ProjectPermission.VIEW_PROJECT
                );

        order.verify(projectRepository)
                .findById(11L);
    }

    @Test
    void getRequiredRequiresViewPermissionBeforeRepositoryLookup() {
        Project project = project(11L, "Website");

        when(projectRepository.findById(11L))
                .thenReturn(Optional.of(project));

        assertThat(projectService.getRequired(11L))
                .isSameAs(project);

        InOrder order = inOrder(
                projectAccessService,
                projectRepository
        );

        order.verify(projectAccessService)
                .requirePermission(
                        11L,
                        ProjectPermission.VIEW_PROJECT
                );

        order.verify(projectRepository)
                .findById(11L);
    }

    @Test
    void getRequiredDoesNotQueryProjectAfterAccessIsHidden() {
        doThrow(
                new ResourceNotFoundException(
                        "Project not found."
                )
        )
                .when(projectAccessService)
                .requirePermission(
                        11L,
                        ProjectPermission.VIEW_PROJECT
                );

        assertThatThrownBy(
                () -> projectService.getRequired(11L)
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage("Project not found.");

        verifyNoInteractions(projectRepository);
    }

    @Test
    void countReturnsAccessibleProjectCount() {
        when(currentCollaboratorResolver.getRequiredId())
                .thenReturn(7L);

        when(projectRepository
                .countAccessibleByCollaboratorIdAndStatus(
                        7L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(3L);

        assertThat(projectService.count())
                .isEqualTo(3L);
    }

    @Test
    void createAssignsCurrentCollaboratorAsOwnerAndManager() {
        Project project = new Project();
        project.setName("  Website  ");
        project.setDescription("  New platform  ");
        project.setStatus(" in_progress ");

        Collaborator creator = collaborator(7L);

        when(currentCollaboratorResolver.getRequired())
                .thenReturn(creator);

        when(projectRepository.saveAndFlush(
                any(Project.class)
        )).thenAnswer(invocation -> {
            Project saved =
                    invocation.getArgument(0);

            saved.setId(21L);
            return saved;
        });

        when(projectMembershipRepository.saveAndFlush(
                any(ProjectMembership.class)
        )).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        Project result = projectService.create(project);

        assertThat(result.getId()).isEqualTo(21L);
        assertThat(result.getName())
                .isEqualTo("Website");
        assertThat(result.getDescription())
                .isEqualTo("New platform");
        assertThat(result.getStatus())
                .isEqualTo("IN_PROGRESS");
        assertThat(result.getManagerId())
                .isEqualTo(7L);

        ArgumentCaptor<ProjectMembership> membershipCaptor =
                ArgumentCaptor.forClass(
                        ProjectMembership.class
                );

        InOrder order = inOrder(
                currentCollaboratorResolver,
                projectRepository,
                projectMembershipRepository
        );

        order.verify(currentCollaboratorResolver)
                .getRequired();

        order.verify(projectRepository)
                .saveAndFlush(project);

        order.verify(projectMembershipRepository)
                .saveAndFlush(
                        membershipCaptor.capture()
                );

        ProjectMembership membership =
                membershipCaptor.getValue();

        assertThat(membership.getProjectId())
                .isEqualTo(21L);

        assertThat(membership.getCollaboratorId())
                .isEqualTo(7L);

        assertThat(membership.getRole())
                .isEqualTo(
                        ProjectMembershipRole.OWNER
                );

        assertThat(membership.getStatus())
                .isEqualTo(
                        ProjectMembershipStatus.ACTIVE
                );

        verifyNoInteractions(collaboratorRepository);
    }

    @Test
    void createRejectsEndDateBeforeStartDate() {
        Project project = new Project();
        project.setName("Website");
        project.setStatus("PLANNED");
        project.setStartDate(
                LocalDate.of(2026, 8, 10)
        );
        project.setEndDate(
                LocalDate.of(2026, 8, 1)
        );

        when(currentCollaboratorResolver.getRequired())
                .thenReturn(collaborator(7L));

        assertThatThrownBy(
                () -> projectService.create(project)
        )
                .isInstanceOf(
                        InvalidOperationException.class
                )
                .hasMessage(
                        "Project end date cannot be " +
                        "before its start date."
                );

        verify(projectRepository, never())
                .saveAndFlush(any(Project.class));

        verifyNoInteractions(
                projectMembershipRepository
        );
    }

    @Test
    void createRejectsDifferentInitialManager() {
        Project project = new Project();
        project.setName("Website");
        project.setStatus("PLANNED");
        project.setManagerId(99L);

        when(currentCollaboratorResolver.getRequired())
                .thenReturn(collaborator(7L));

        assertThatThrownBy(
                () -> projectService.create(project)
        )
                .isInstanceOf(
                        InvalidOperationException.class
                )
                .hasMessage(
                        "The project creator must be " +
                        "the initial project manager."
                );

        verifyNoInteractions(
                projectRepository,
                projectMembershipRepository,
                collaboratorRepository
        );
    }

    @Test
    void updateRequiresManagePermissionBeforeLoadingAndSaving() {
        Project existing = project(11L, "Old name");
        Project updated = project(null, "  New name  ");
        updated.setStatus(" in_progress ");

        when(projectRepository.findById(11L))
                .thenReturn(Optional.of(existing));

        when(projectRepository.save(existing))
                .thenReturn(existing);

        Project result =
                projectService.update(11L, updated);

        assertThat(result.getName())
                .isEqualTo("New name");

        assertThat(result.getStatus())
                .isEqualTo("IN_PROGRESS");

        InOrder order = inOrder(
                projectAccessService,
                projectRepository
        );

        order.verify(projectAccessService)
                .requirePermission(
                        11L,
                        ProjectPermission.MANAGE_PROJECT
                );

        order.verify(projectRepository)
                .findById(11L);

        order.verify(projectRepository)
                .save(existing);
    }

    @Test
    void updateDoesNotLoadOrSaveProjectWhenPermissionIsDenied() {
        doThrow(
                new ResourceNotFoundException(
                        "Project not found."
                )
        )
                .when(projectAccessService)
                .requirePermission(
                        11L,
                        ProjectPermission.MANAGE_PROJECT
                );

        assertThatThrownBy(
                () -> projectService.update(
                        11L,
                        project(null, "Updated")
                )
        ).isInstanceOf(
                ResourceNotFoundException.class
        );

        verify(projectRepository, never())
                .findById(any(Long.class));

        verify(projectRepository, never())
                .save(any());
    }

    @Test
    void updateAcceptsActiveManagerMembershipAsProjectManager() {
        Project existing = project(11L, "Old name");
        existing.setManagerId(7L);

        Project updated = project(null, "New name");
        updated.setManagerId(8L);

        Collaborator manager = collaborator(8L);
        manager.setActive(true);

        ProjectMembership membership = new ProjectMembership();
        membership.setProjectId(11L);
        membership.setCollaboratorId(8L);
        membership.setRole(ProjectMembershipRole.MANAGER);
        membership.setStatus(ProjectMembershipStatus.ACTIVE);

        when(projectRepository.findById(11L))
                .thenReturn(Optional.of(existing));
        when(collaboratorRepository.findById(8L))
                .thenReturn(Optional.of(manager));
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(membership));
        when(projectRepository.save(existing))
                .thenReturn(existing);

        Project result = projectService.update(11L, updated);

        assertThat(result.getManagerId()).isEqualTo(8L);
    }

    @Test
    void updateRejectsInactiveProjectManager() {
        Project existing = project(11L, "Old name");
        Project updated = project(null, "New name");
        updated.setManagerId(8L);

        Collaborator inactive = collaborator(8L);
        inactive.setActive(false);

        when(projectRepository.findById(11L))
                .thenReturn(Optional.of(existing));
        when(collaboratorRepository.findById(8L))
                .thenReturn(Optional.of(inactive));

        assertThatThrownBy(() ->
                projectService.update(11L, updated)
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "The selected project manager must be active."
                );

        verifyNoInteractions(projectMembershipRepository);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateRejectsManagerWhoIsNotActiveProjectMember() {
        Project existing = project(11L, "Old name");
        Project updated = project(null, "New name");
        updated.setManagerId(8L);

        Collaborator manager = collaborator(8L);
        manager.setActive(true);

        when(projectRepository.findById(11L))
                .thenReturn(Optional.of(existing));
        when(collaboratorRepository.findById(8L))
                .thenReturn(Optional.of(manager));
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                projectService.update(11L, updated)
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "The selected project manager must be an active member of this project."
                );

        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateRejectsContributorAsProjectManager() {
        Project existing = project(11L, "Old name");
        Project updated = project(null, "New name");
        updated.setManagerId(8L);

        Collaborator collaborator = collaborator(8L);
        collaborator.setActive(true);

        ProjectMembership membership = new ProjectMembership();
        membership.setProjectId(11L);
        membership.setCollaboratorId(8L);
        membership.setRole(ProjectMembershipRole.CONTRIBUTOR);
        membership.setStatus(ProjectMembershipStatus.ACTIVE);

        when(projectRepository.findById(11L))
                .thenReturn(Optional.of(existing));
        when(collaboratorRepository.findById(8L))
                .thenReturn(Optional.of(collaborator));
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() ->
                projectService.update(11L, updated)
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "The selected project manager must have the OWNER or MANAGER project role."
                );

        verify(projectRepository, never()).save(any());
    }

    @Test
    void deleteRequiresOwnerPermissionBeforeLoadingAndDeleting() {
        Project project = project(11L, "Website");

        when(projectRepository.findById(11L))
                .thenReturn(Optional.of(project));

        projectService.delete(11L);

        InOrder order = inOrder(
                projectAccessService,
                projectRepository
        );

        order.verify(projectAccessService)
                .requirePermission(
                        11L,
                        ProjectPermission.DELETE_PROJECT
                );

        order.verify(projectRepository)
                .findById(11L);

        order.verify(projectRepository)
                .delete(project);
    }

    private Project project(
            Long id,
            String name
    ) {
        Project project = new Project();

        project.setId(id);
        project.setName(name);
        project.setStatus("PLANNED");

        return project;
    }

    private Collaborator collaborator(Long id) {
        Collaborator collaborator =
                new Collaborator();

        collaborator.setId(id);

        return collaborator;
    }
}