package com.devflowhub.backend.service;

import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.entity.Project;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import com.devflowhub.backend.security.CurrentCollaboratorResolver;
import com.devflowhub.backend.security.ProjectAccessService;
import com.devflowhub.backend.security.ProjectPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private CurrentCollaboratorResolver currentCollaboratorResolver;

    @Mock
    private ProjectAccessService projectAccessService;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(
                projectRepository,
                collaboratorRepository,
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
        doThrow(new ResourceNotFoundException("Project not found."))
                .when(projectAccessService)
                .requirePermission(
                        11L,
                        ProjectPermission.VIEW_PROJECT
                );

        assertThatThrownBy(() -> projectService.getRequired(11L))
                .isInstanceOf(ResourceNotFoundException.class)
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

        assertThat(projectService.count()).isEqualTo(3L);
    }

    @Test
    void createNormalizesStatusAndText() {
        Project project = new Project();
        project.setName("  Website  ");
        project.setDescription("  New platform  ");
        project.setStatus(" in_progress ");
        project.setManagerId(2L);

        when(collaboratorRepository.existsById(2L)).thenReturn(true);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Project result = projectService.create(project);

        assertThat(result.getName()).isEqualTo("Website");
        assertThat(result.getDescription()).isEqualTo("New platform");
        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void createRejectsEndDateBeforeStartDate() {
        Project project = new Project();
        project.setName("Website");
        project.setStatus("PLANNED");
        project.setStartDate(LocalDate.of(2026, 8, 10));
        project.setEndDate(LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> projectService.create(project))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Project end date cannot be before its start date.");
    }

    @Test
    void createRejectsUnknownManager() {
        Project project = new Project();
        project.setName("Website");
        project.setStatus("PLANNED");
        project.setManagerId(99L);

        when(collaboratorRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> projectService.create(project))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("The selected project manager does not exist.");
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

        Project result = projectService.update(11L, updated);

        assertThat(result.getName()).isEqualTo("New name");
        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");

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
        doThrow(new ResourceNotFoundException("Project not found."))
                .when(projectAccessService)
                .requirePermission(
                        11L,
                        ProjectPermission.MANAGE_PROJECT
                );

        assertThatThrownBy(() -> projectService.update(
                11L,
                project(null, "Updated")
        )).isInstanceOf(ResourceNotFoundException.class);

        verify(projectRepository, never()).findById(any(Long.class));
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

    private Project project(Long id, String name) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setStatus("PLANNED");
        return project;
    }
}
