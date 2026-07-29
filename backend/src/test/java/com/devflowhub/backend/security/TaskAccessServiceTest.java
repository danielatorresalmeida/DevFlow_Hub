package com.devflowhub.backend.security;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.entity.ProjectMembership;
import com.devflowhub.backend.entity.Task;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ProjectAccessDeniedException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.ProjectMembershipRepository;
import com.devflowhub.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskAccessServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CurrentCollaboratorResolver
            currentCollaboratorResolver;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private ProjectMembershipRepository
            projectMembershipRepository;

    private TaskAccessService taskAccessService;

    @BeforeEach
    void setUp() {
        taskAccessService = new TaskAccessService(
                taskRepository,
                currentCollaboratorResolver,
                projectAccessService,
                projectMembershipRepository
        );
    }

    @Test
    void nullTaskIdIsHiddenWithoutRepositoryLookup() {
        assertThatThrownBy(
                () -> taskAccessService
                        .getRequiredForView(null)
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage("Task not found.");

        verifyNoInteractions(
                taskRepository,
                currentCollaboratorResolver,
                projectAccessService
        );
    }

    @Test
    void missingTaskIsHidden() {
        when(taskRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> taskAccessService
                        .getRequiredForView(99L)
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage("Task not found.");

        verifyNoInteractions(
                currentCollaboratorResolver,
                projectAccessService
        );
    }

    @Test
    void projectTaskViewRequiresProjectViewPermission() {
        Task task = projectTask(
                11L,
                4L,
                8L
        );

        ProjectMembership membership =
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.VIEWER
                );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(membership);

        assertThat(
                taskAccessService
                        .getRequiredForView(11L)
        ).isSameAs(task);

        verify(projectAccessService)
                .requirePermission(
                        4L,
                        ProjectPermission.VIEW_PROJECT
                );

        verifyNoInteractions(
                currentCollaboratorResolver
        );
    }

    @Test
    void hiddenProjectIsReportedAsHiddenTask() {
        Task task = projectTask(
                11L,
                4L,
                8L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenThrow(
                new ResourceNotFoundException(
                        "Project not found."
                )
        );

        assertThatThrownBy(
                () -> taskAccessService
                        .getRequiredForView(11L)
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage("Task not found.");

        verifyNoInteractions(
                currentCollaboratorResolver
        );
    }

    @Test
    void standaloneAssigneeCanViewTask() {
        Task task = standaloneTask(
                11L,
                7L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        assertThat(
                taskAccessService
                        .getRequiredForView(11L)
        ).isSameAs(task);

        verifyNoInteractions(projectAccessService);
    }

    @Test
    void otherCollaboratorCannotViewStandaloneTask() {
        Task task = standaloneTask(
                11L,
                8L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        assertThatThrownBy(
                () -> taskAccessService
                        .getRequiredForView(11L)
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage("Task not found.");

        verifyNoInteractions(projectAccessService);
    }

    @Test
    void ownerCanManageAnyProjectTask() {
        Task task = projectTask(
                11L,
                4L,
                8L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.OWNER
                )
        );

        assertThat(
                taskAccessService
                        .getRequiredForManagement(11L)
        ).isSameAs(task);
    }

    @Test
    void managerCanManageAnyProjectTask() {
        Task task = projectTask(
                11L,
                4L,
                8L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.MANAGER
                )
        );

        assertThat(
                taskAccessService
                        .getRequiredForManagement(11L)
        ).isSameAs(task);
    }

    @Test
    void contributorCanManageAssignedProjectTask() {
        Task task = projectTask(
                11L,
                4L,
                7L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.CONTRIBUTOR
                )
        );

        assertThat(
                taskAccessService
                        .getRequiredForManagement(11L)
        ).isSameAs(task);
    }

    @Test
    void contributorCannotManageAnotherProjectTask() {
        Task task = projectTask(
                11L,
                4L,
                8L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.CONTRIBUTOR
                )
        );

        assertThatThrownBy(
                () -> taskAccessService
                        .getRequiredForManagement(11L)
        ).isInstanceOf(
                ProjectAccessDeniedException.class
        );
    }

    @Test
    void viewerCannotManageProjectTask() {
        Task task = projectTask(
                11L,
                4L,
                7L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.VIEWER
                )
        );

        assertThatThrownBy(
                () -> taskAccessService
                        .getRequiredForManagement(11L)
        ).isInstanceOf(
                ProjectAccessDeniedException.class
        );
    }

    @Test
    void assignedProjectMemberCanPerformAssigneeAction() {
        Task task = projectTask(
                11L,
                4L,
                7L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.CONTRIBUTOR
                )
        );

        assertThat(
                taskAccessService
                        .getRequiredForAssigneeAction(11L)
        ).isSameAs(task);
    }

    @Test
    void otherProjectMemberCannotPerformAssigneeAction() {
        Task task = projectTask(
                11L,
                4L,
                8L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.MANAGER
                )
        );

        assertThatThrownBy(
                () -> taskAccessService
                        .getRequiredForAssigneeAction(11L)
        ).isInstanceOf(
                ProjectAccessDeniedException.class
        );
    }

    @Test
    void standaloneAssigneeCanManageTask() {
        Task task = standaloneTask(
                11L,
                7L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        assertThat(
                taskAccessService
                        .getRequiredForManagement(11L)
        ).isSameAs(task);

        verifyNoInteractions(projectAccessService);
    }

    @Test
    void otherCollaboratorCannotPerformStandaloneAssigneeAction() {
        Task task = standaloneTask(
                11L,
                8L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        assertThatThrownBy(
                () -> taskAccessService
                        .getRequiredForAssigneeAction(11L)
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage("Task not found.");

        verifyNoInteractions(projectAccessService);
    }

    @Test
    void viewerAssignedToProjectTaskCanPerformAssigneeAction() {
        Task task = projectTask(
                11L,
                4L,
                7L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.VIEWER
                )
        );

        assertThat(
                taskAccessService
                        .getRequiredForAssigneeAction(11L)
        ).isSameAs(task);
    }

    @Test
    void standaloneAssigneeCanPerformAssigneeAction() {
        Task task = standaloneTask(
                11L,
                7L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        assertThat(
                taskAccessService
                        .getRequiredForAssigneeAction(11L)
        ).isSameAs(task);

        verifyNoInteractions(projectAccessService);
    }

    @Test
    void otherCollaboratorCannotManageStandaloneTask() {
        Task task = standaloneTask(
                11L,
                8L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(task));

        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        assertThatThrownBy(
                () -> taskAccessService
                        .getRequiredForManagement(11L)
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage("Task not found.");

        verifyNoInteractions(projectAccessService);
    }

    @Test
    void standaloneTaskWithoutAssigneeIsAssignedToCurrentCollaborator() {
        Task task = standaloneTask(
                null,
                null
        );

        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        taskAccessService.prepareForCreate(task);

        assertThat(task.getAssigneeId())
                .isEqualTo(7L);

        verifyNoInteractions(
                taskRepository,
                projectAccessService,
                projectMembershipRepository
        );
    }

    @Test
    void standaloneTaskAssignedToCurrentCollaboratorIsAccepted() {
        Task task = standaloneTask(
                null,
                7L
        );

        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        taskAccessService.prepareForCreate(task);

        assertThat(task.getAssigneeId())
                .isEqualTo(7L);

        verifyNoInteractions(
                taskRepository,
                projectAccessService,
                projectMembershipRepository
        );
    }

    @Test
    void standaloneTaskAssignedToOtherCollaboratorIsRejected() {
        Task task = standaloneTask(
                null,
                8L
        );

        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        assertThatThrownBy(
                () -> taskAccessService
                        .prepareForCreate(task)
        )
                .isInstanceOf(
                        InvalidOperationException.class
                )
                .hasMessage(
                        "A standalone task must be assigned " +
                        "to the authenticated collaborator."
                );

        verifyNoInteractions(
                taskRepository,
                projectAccessService,
                projectMembershipRepository
        );
    }

    @Test
    void ownerCanCreateUnassignedProjectTask() {
        Task task = projectTask(
                null,
                4L,
                null
        );

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.CONTRIBUTE_TO_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.OWNER
                )
        );

        taskAccessService.prepareForCreate(task);

        assertThat(task.getAssigneeId())
                .isNull();

        verify(projectAccessService)
                .requirePermission(
                        4L,
                        ProjectPermission
                                .CONTRIBUTE_TO_PROJECT
                );

        verifyNoInteractions(
                taskRepository,
                currentCollaboratorResolver,
                projectMembershipRepository
        );
    }

    @Test
    void managerCanAssignActiveProjectMember() {
        Task task = projectTask(
                null,
                4L,
                8L
        );

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.CONTRIBUTE_TO_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.MANAGER
                )
        );

        when(projectMembershipRepository
                .existsByProjectIdAndCollaboratorIdAndStatus(
                        4L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(true);

        taskAccessService.prepareForCreate(task);

        verify(projectMembershipRepository)
                .existsByProjectIdAndCollaboratorIdAndStatus(
                        4L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                );
    }

    @Test
    void contributorWithoutAssigneeIsAssignedToSelf() {
        Task task = projectTask(
                null,
                4L,
                null
        );

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.CONTRIBUTE_TO_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.CONTRIBUTOR
                )
        );

        taskAccessService.prepareForCreate(task);

        assertThat(task.getAssigneeId())
                .isEqualTo(7L);

        verifyNoInteractions(
                taskRepository,
                currentCollaboratorResolver,
                projectMembershipRepository
        );
    }

    @Test
    void contributorCanAssignProjectTaskToSelf() {
        Task task = projectTask(
                null,
                4L,
                7L
        );

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.CONTRIBUTE_TO_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.CONTRIBUTOR
                )
        );

        taskAccessService.prepareForCreate(task);

        verifyNoInteractions(
                taskRepository,
                currentCollaboratorResolver,
                projectMembershipRepository
        );
    }

    @Test
    void contributorCannotAssignProjectTaskToAnotherMember() {
        Task task = projectTask(
                null,
                4L,
                8L
        );

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.CONTRIBUTE_TO_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.CONTRIBUTOR
                )
        );

        assertThatThrownBy(
                () -> taskAccessService
                        .prepareForCreate(task)
        ).isInstanceOf(
                ProjectAccessDeniedException.class
        );

        verifyNoInteractions(
                taskRepository,
                currentCollaboratorResolver,
                projectMembershipRepository
        );
    }

    @Test
    void projectAssigneeMustBeActiveProjectMember() {
        Task task = projectTask(
                null,
                4L,
                8L
        );

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.CONTRIBUTE_TO_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.MANAGER
                )
        );

        when(projectMembershipRepository
                .existsByProjectIdAndCollaboratorIdAndStatus(
                        4L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(false);

        assertThatThrownBy(
                () -> taskAccessService
                        .prepareForCreate(task)
        )
                .isInstanceOf(
                        InvalidOperationException.class
                )
                .hasMessage(
                        "The selected assignee must be " +
                        "an active project member."
                );
    }

    @Test
    void viewerCannotCreateProjectTask() {
        Task task = projectTask(
                null,
                4L,
                7L
        );

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.CONTRIBUTE_TO_PROJECT
        )).thenThrow(
                new ProjectAccessDeniedException()
        );

        assertThatThrownBy(
                () -> taskAccessService
                        .prepareForCreate(task)
        ).isInstanceOf(
                ProjectAccessDeniedException.class
        );

        verifyNoInteractions(
                taskRepository,
                currentCollaboratorResolver,
                projectMembershipRepository
        );
    }

    @Test
    void ownerCanUpdateProjectTaskAndAssignActiveMember() {
        Task existing = projectTask(
                11L,
                4L,
                8L
        );

        Task updated = projectTask(
                null,
                4L,
                9L
        );

        ProjectMembership owner =
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.OWNER
                );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(existing));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(owner);

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.CONTRIBUTE_TO_PROJECT
        )).thenReturn(owner);

        when(projectMembershipRepository
                .existsByProjectIdAndCollaboratorIdAndStatus(
                        4L,
                        9L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(true);

        assertThat(
                taskAccessService
                        .getRequiredAndPrepareForUpdate(
                                11L,
                                updated
                        )
        ).isSameAs(existing);

        InOrder order = inOrder(
                projectAccessService,
                projectMembershipRepository
        );

        order.verify(projectAccessService)
                .requirePermission(
                        4L,
                        ProjectPermission.VIEW_PROJECT
                );

        order.verify(projectAccessService)
                .requirePermission(
                        4L,
                        ProjectPermission
                                .CONTRIBUTE_TO_PROJECT
                );

        order.verify(projectMembershipRepository)
                .existsByProjectIdAndCollaboratorIdAndStatus(
                        4L,
                        9L,
                        ProjectMembershipStatus.ACTIVE
                );

        verifyNoInteractions(
                currentCollaboratorResolver
        );
    }

    @Test
    void managerCanMoveTaskToAnotherProject() {
        Task existing = projectTask(
                11L,
                4L,
                8L
        );

        Task updated = projectTask(
                null,
                5L,
                9L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(existing));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.MANAGER
                )
        );

        when(projectAccessService.requirePermission(
                5L,
                ProjectPermission.CONTRIBUTE_TO_PROJECT
        )).thenReturn(
                membership(
                        5L,
                        7L,
                        ProjectMembershipRole.MANAGER
                )
        );

        when(projectMembershipRepository
                .existsByProjectIdAndCollaboratorIdAndStatus(
                        5L,
                        9L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(true);

        assertThat(
                taskAccessService
                        .getRequiredAndPrepareForUpdate(
                                11L,
                                updated
                        )
        ).isSameAs(existing);

        InOrder order = inOrder(
                projectAccessService,
                projectMembershipRepository
        );

        order.verify(projectAccessService)
                .requirePermission(
                        4L,
                        ProjectPermission.VIEW_PROJECT
                );

        order.verify(projectAccessService)
                .requirePermission(
                        5L,
                        ProjectPermission
                                .CONTRIBUTE_TO_PROJECT
                );

        order.verify(projectMembershipRepository)
                .existsByProjectIdAndCollaboratorIdAndStatus(
                        5L,
                        9L,
                        ProjectMembershipStatus.ACTIVE
                );
    }

    @Test
    void contributorCanMoveAssignedTaskAndRemainAssignedToSelf() {
        Task existing = projectTask(
                11L,
                4L,
                7L
        );

        Task updated = projectTask(
                null,
                5L,
                7L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(existing));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.CONTRIBUTOR
                )
        );

        when(projectAccessService.requirePermission(
                5L,
                ProjectPermission.CONTRIBUTE_TO_PROJECT
        )).thenReturn(
                membership(
                        5L,
                        7L,
                        ProjectMembershipRole.CONTRIBUTOR
                )
        );

        assertThat(
                taskAccessService
                        .getRequiredAndPrepareForUpdate(
                                11L,
                                updated
                        )
        ).isSameAs(existing);

        assertThat(updated.getAssigneeId())
                .isEqualTo(7L);

        verifyNoInteractions(
                currentCollaboratorResolver,
                projectMembershipRepository
        );
    }

    @Test
    void contributorWithoutDestinationAssigneeIsAssignedToSelf() {
        Task existing = projectTask(
                11L,
                4L,
                7L
        );

        Task updated = projectTask(
                null,
                5L,
                null
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(existing));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.CONTRIBUTOR
                )
        );

        when(projectAccessService.requirePermission(
                5L,
                ProjectPermission.CONTRIBUTE_TO_PROJECT
        )).thenReturn(
                membership(
                        5L,
                        7L,
                        ProjectMembershipRole.CONTRIBUTOR
                )
        );

        taskAccessService
                .getRequiredAndPrepareForUpdate(
                        11L,
                        updated
                );

        assertThat(updated.getAssigneeId())
                .isEqualTo(7L);

        verifyNoInteractions(
                currentCollaboratorResolver,
                projectMembershipRepository
        );
    }

    @Test
    void contributorCannotAssignUpdatedTaskToAnotherMember() {
        Task existing = projectTask(
                11L,
                4L,
                7L
        );

        Task updated = projectTask(
                null,
                5L,
                8L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(existing));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.CONTRIBUTOR
                )
        );

        when(projectAccessService.requirePermission(
                5L,
                ProjectPermission.CONTRIBUTE_TO_PROJECT
        )).thenReturn(
                membership(
                        5L,
                        7L,
                        ProjectMembershipRole.CONTRIBUTOR
                )
        );

        assertThatThrownBy(
                () -> taskAccessService
                        .getRequiredAndPrepareForUpdate(
                                11L,
                                updated
                        )
        ).isInstanceOf(
                ProjectAccessDeniedException.class
        );

        verifyNoInteractions(
                currentCollaboratorResolver,
                projectMembershipRepository
        );
    }

    @Test
    void viewerCannotUpdateBeforeDestinationAuthorization() {
        Task existing = projectTask(
                11L,
                4L,
                7L
        );

        Task updated = projectTask(
                null,
                5L,
                7L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(existing));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.VIEWER
                )
        );

        assertThatThrownBy(
                () -> taskAccessService
                        .getRequiredAndPrepareForUpdate(
                                11L,
                                updated
                        )
        ).isInstanceOf(
                ProjectAccessDeniedException.class
        );

        verify(projectAccessService)
                .requirePermission(
                        4L,
                        ProjectPermission.VIEW_PROJECT
                );

        verifyNoInteractions(
                currentCollaboratorResolver,
                projectMembershipRepository
        );
    }

    @Test
    void projectTaskCanBecomeStandaloneForCurrentCollaborator() {
        Task existing = projectTask(
                11L,
                4L,
                8L
        );

        Task updated = standaloneTask(
                null,
                null
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(existing));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.MANAGER
                )
        );

        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        assertThat(
                taskAccessService
                        .getRequiredAndPrepareForUpdate(
                                11L,
                                updated
                        )
        ).isSameAs(existing);

        assertThat(updated.getAssigneeId())
                .isEqualTo(7L);

        verifyNoInteractions(
                projectMembershipRepository
        );
    }

    @Test
    void projectTaskCannotBecomeStandaloneForAnotherCollaborator() {
        Task existing = projectTask(
                11L,
                4L,
                8L
        );

        Task updated = standaloneTask(
                null,
                9L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(existing));

        when(projectAccessService.requirePermission(
                4L,
                ProjectPermission.VIEW_PROJECT
        )).thenReturn(
                membership(
                        4L,
                        7L,
                        ProjectMembershipRole.MANAGER
                )
        );

        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        assertThatThrownBy(
                () -> taskAccessService
                        .getRequiredAndPrepareForUpdate(
                                11L,
                                updated
                        )
        )
                .isInstanceOf(
                        InvalidOperationException.class
                )
                .hasMessage(
                        "A standalone task must be assigned " +
                        "to the authenticated collaborator."
                );

        verifyNoInteractions(
                projectMembershipRepository
        );
    }

    @Test
    void standaloneAssigneeCanMoveTaskToProject() {
        Task existing = standaloneTask(
                11L,
                7L
        );

        Task updated = projectTask(
                null,
                5L,
                7L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(existing));

        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        when(projectAccessService.requirePermission(
                5L,
                ProjectPermission.CONTRIBUTE_TO_PROJECT
        )).thenReturn(
                membership(
                        5L,
                        7L,
                        ProjectMembershipRole.MANAGER
                )
        );

        assertThat(
                taskAccessService
                        .getRequiredAndPrepareForUpdate(
                                11L,
                                updated
                        )
        ).isSameAs(existing);

        verifyNoInteractions(
                projectMembershipRepository
        );
    }

    @Test
    void otherCollaboratorCannotMoveStandaloneTask() {
        Task existing = standaloneTask(
                11L,
                8L
        );

        Task updated = projectTask(
                null,
                5L,
                7L
        );

        when(taskRepository.findById(11L))
                .thenReturn(Optional.of(existing));

        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        assertThatThrownBy(
                () -> taskAccessService
                        .getRequiredAndPrepareForUpdate(
                                11L,
                                updated
                        )
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage("Task not found.");

        verifyNoInteractions(
                projectAccessService,
                projectMembershipRepository
        );
    }

    private Task projectTask(
            Long id,
            Long projectId,
            Long assigneeId
    ) {
        Task task = new Task();

        task.setId(id);
        task.setProjectId(projectId);
        task.setAssigneeId(assigneeId);

        return task;
    }

    private Task standaloneTask(
            Long id,
            Long assigneeId
    ) {
        Task task = new Task();

        task.setId(id);
        task.setProjectId(null);
        task.setAssigneeId(assigneeId);

        return task;
    }

    private ProjectMembership membership(
            Long projectId,
            Long collaboratorId,
            ProjectMembershipRole role
    ) {
        ProjectMembership membership =
                new ProjectMembership();

        membership.setProjectId(projectId);
        membership.setCollaboratorId(
                collaboratorId
        );
        membership.setRole(role);
        membership.setStatus(
                ProjectMembershipStatus.ACTIVE
        );

        return membership;
    }
}
