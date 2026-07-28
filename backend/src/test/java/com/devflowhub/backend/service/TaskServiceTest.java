package com.devflowhub.backend.service;

import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.entity.Task;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import com.devflowhub.backend.repository.TaskRepository;
import com.devflowhub.backend.security.CurrentCollaboratorResolver;
import com.devflowhub.backend.security.TaskAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CollaboratorRepository collaboratorRepository;

    @Mock
    private CurrentCollaboratorResolver
            currentCollaboratorResolver;

    @Mock
    private TaskAccessService taskAccessService;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-07-14T12:00:00Z"),
                ZoneOffset.UTC
        );

        taskService = new TaskService(
                taskRepository,
                projectRepository,
                collaboratorRepository,
                fixedClock,
                currentCollaboratorResolver,
                taskAccessService
        );
    }

    @Test
    void findAllReturnsOnlyTasksAccessibleToCurrentCollaborator() {
        Task projectTask = new Task();
        projectTask.setId(11L);
        projectTask.setProjectId(4L);

        Task standaloneTask = new Task();
        standaloneTask.setId(12L);
        standaloneTask.setAssigneeId(7L);

        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        when(taskRepository
                .findAccessibleByCollaboratorIdAndStatus(
                        7L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(
                        List.of(
                                projectTask,
                                standaloneTask
                        )
                );

        assertThat(taskService.findAll())
                .containsExactly(
                        projectTask,
                        standaloneTask
                );

        verify(taskRepository)
                .findAccessibleByCollaboratorIdAndStatus(
                        7L,
                        ProjectMembershipStatus.ACTIVE
                );

        verifyNoInteractions(taskAccessService);
    }

    @Test
    void findByIdDelegatesToViewAccess() {
        Task task = new Task();
        task.setId(11L);

        when(taskAccessService
                .getRequiredForView(11L))
                .thenReturn(task);

        assertThat(taskService.findById(11L))
                .contains(task);

        verify(taskAccessService)
                .getRequiredForView(11L);

        verifyNoInteractions(taskRepository);
    }

    @Test
    void getRequiredDelegatesToViewAccess() {
        Task task = new Task();
        task.setId(11L);

        when(taskAccessService
                .getRequiredForView(11L))
                .thenReturn(task);

        assertThat(taskService.getRequired(11L))
                .isSameAs(task);

        verify(taskAccessService)
                .getRequiredForView(11L);

        verifyNoInteractions(taskRepository);
    }

    @Test
    void countReturnsOnlyAccessibleTaskCount() {
        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        when(taskRepository
                .countAccessibleByCollaboratorIdAndStatus(
                        7L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(3L);

        assertThat(taskService.count())
                .isEqualTo(3L);

        verify(taskRepository)
                .countAccessibleByCollaboratorIdAndStatus(
                        7L,
                        ProjectMembershipStatus.ACTIVE
                );
    }

    @Test
    void countByStatusReturnsOnlyAccessibleTaskCount() {
        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        when(taskRepository
                .countAccessibleByCollaboratorIdAndStatusAndTaskStatus(
                        7L,
                        ProjectMembershipStatus.ACTIVE,
                        "PENDING"
                ))
                .thenReturn(2L);

        assertThat(
                taskService.countByStatus("PENDING")
        ).isEqualTo(2L);

        verify(taskRepository)
                .countAccessibleByCollaboratorIdAndStatusAndTaskStatus(
                        7L,
                        ProjectMembershipStatus.ACTIVE,
                        "PENDING"
                );
    }

    @Test
    void getStoredTimeSecondsReturnsOnlyAccessibleTime() {
        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        when(taskRepository
                .sumAccessibleStoredTimeSecondsByCollaboratorIdAndStatus(
                        7L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(180L);

        assertThat(
                taskService.getStoredTimeSeconds()
        ).isEqualTo(180L);

        verify(taskRepository)
                .sumAccessibleStoredTimeSecondsByCollaboratorIdAndStatus(
                        7L,
                        ProjectMembershipStatus.ACTIVE
                );
    }

    @Test
    void getStoredTimeSecondsHandlesNullRepositoryResult() {
        when(currentCollaboratorResolver
                .getRequiredId())
                .thenReturn(7L);

        when(taskRepository
                .sumAccessibleStoredTimeSecondsByCollaboratorIdAndStatus(
                        7L,
                        ProjectMembershipStatus.ACTIVE
                ))
                .thenReturn(null);

        assertThat(
                taskService.getStoredTimeSeconds()
        ).isZero();
    }

    @Test
    void createAuthorizesParentBeforeValidationAndSave() {
        Task task = new Task();

        task.setTitle("Create secured task");
        task.setStatus("PENDING");
        task.setPriority("MEDIUM");
        task.setProjectId(4L);
        task.setAssigneeId(8L);

        when(projectRepository.existsById(4L))
                .thenReturn(true);

        when(collaboratorRepository.existsById(8L))
                .thenReturn(true);

        when(taskRepository.save(task))
                .thenReturn(task);

        assertThat(taskService.create(task))
                .isSameAs(task);

        InOrder order = inOrder(
                taskAccessService,
                projectRepository,
                collaboratorRepository,
                taskRepository
        );

        order.verify(taskAccessService)
                .prepareForCreate(task);

        order.verify(projectRepository)
                .existsById(4L);

        order.verify(collaboratorRepository)
                .existsById(8L);

        order.verify(taskRepository)
                .save(task);
    }

    @Test
    void createIgnoresClientSuppliedAuditFields() {
        Task task = new Task();
        task.setTitle("Audit task");
        task.setStatus("PENDING");
        task.setPriority("MEDIUM");
        task.setCreatedAt(LocalDateTime.of(2000, 1, 1, 0, 0));
        task.setUpdatedAt(LocalDateTime.of(2000, 1, 2, 0, 0));

        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        taskService.create(task);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());

        assertThat(captor.getValue().getCreatedAt()).isNull();
        assertThat(captor.getValue().getUpdatedAt()).isNull();
    }

    @Test
    void updateAuthorizesBeforeValidationAndSave() {
        Task existing = new Task();

        existing.setId(11L);
        existing.setTitle("Original task");
        existing.setStatus("PENDING");
        existing.setPriority("MEDIUM");
        existing.setProjectId(4L);
        existing.setAssigneeId(8L);
        existing.setTimerActive(false);

        Task updated = new Task();

        updated.setTitle("Updated task");
        updated.setDescription("Updated description");
        updated.setStatus("REVIEW");
        updated.setPriority("HIGH");
        updated.setProjectId(5L);
        updated.setAssigneeId(9L);

        when(taskAccessService
                .getRequiredAndPrepareForUpdate(
                        11L,
                        updated
                ))
                .thenReturn(existing);

        when(projectRepository.existsById(5L))
                .thenReturn(true);

        when(collaboratorRepository.existsById(9L))
                .thenReturn(true);

        when(taskRepository.save(existing))
                .thenReturn(existing);

        Task result = taskService.update(
                11L,
                updated
        );

        assertThat(result)
                .isSameAs(existing);

        assertThat(existing.getTitle())
                .isEqualTo("Updated task");

        assertThat(existing.getDescription())
                .isEqualTo("Updated description");

        assertThat(existing.getStatus())
                .isEqualTo("REVIEW");

        assertThat(existing.getPriority())
                .isEqualTo("HIGH");

        assertThat(existing.getProjectId())
                .isEqualTo(5L);

        assertThat(existing.getAssigneeId())
                .isEqualTo(9L);

        InOrder order = inOrder(
                taskAccessService,
                projectRepository,
                collaboratorRepository,
                taskRepository
        );

        order.verify(taskAccessService)
                .getRequiredAndPrepareForUpdate(
                        11L,
                        updated
                );

        order.verify(projectRepository)
                .existsById(5L);

        order.verify(collaboratorRepository)
                .existsById(9L);

        order.verify(taskRepository)
                .save(existing);
    }

    @Test
    void updatePreservesStatusAndTimerStateWhileTimerIsActive() {
        Task existing = new Task();

        existing.setId(11L);
        existing.setTitle("Running task");
        existing.setStatus("IN_PROGRESS");
        existing.setPriority("MEDIUM");
        existing.setProjectId(4L);
        existing.setAssigneeId(7L);
        existing.setTimerActive(true);
        existing.setTimerStartedAt(
                LocalDateTime.of(
                        2026,
                        7,
                        14,
                        11,
                        55
                )
        );
        existing.setTotalTimeSeconds(30L);

        Task updated = new Task();

        updated.setTitle("Renamed running task");
        updated.setStatus("COMPLETED");
        updated.setPriority("HIGH");
        updated.setProjectId(4L);
        updated.setAssigneeId(7L);

        when(taskAccessService
                .getRequiredAndPrepareForUpdate(
                        11L,
                        updated
                ))
                .thenReturn(existing);

        when(projectRepository.existsById(4L))
                .thenReturn(true);

        when(collaboratorRepository.existsById(7L))
                .thenReturn(true);

        when(taskRepository.save(existing))
                .thenReturn(existing);

        Task result = taskService.update(
                11L,
                updated
        );

        assertThat(result.getTitle())
                .isEqualTo("Renamed running task");

        assertThat(result.getPriority())
                .isEqualTo("HIGH");

        assertThat(result.getStatus())
                .isEqualTo("IN_PROGRESS");

        assertThat(result.getTimerActive())
                .isTrue();

        assertThat(result.getTimerStartedAt())
                .isEqualTo(
                        LocalDateTime.of(
                                2026,
                                7,
                                14,
                                11,
                                55
                        )
                );

        assertThat(result.getTotalTimeSeconds())
                .isEqualTo(30L);
    }

    @Test
    void getTotalTimeRequiresViewAccess() {
        Task task = new Task();

        task.setId(1L);
        task.setTimerActive(true);
        task.setTimerStartedAt(
                LocalDateTime.of(
                        2026,
                        7,
                        14,
                        11,
                        58,
                        30
                )
        );
        task.setTotalTimeSeconds(30L);

        when(taskAccessService
                .getRequiredForView(1L))
                .thenReturn(task);

        assertThat(
                taskService.getTotalTime(1L)
        ).isEqualTo(120L);

        verify(taskAccessService)
                .getRequiredForView(1L);

        verifyNoInteractions(
                taskRepository,
                projectRepository,
                collaboratorRepository,
                currentCollaboratorResolver
        );
    }

    @Test
    void getTimerRequiresViewAccess() {
        Task task = new Task();

        task.setId(1L);
        task.setStatus("IN_PROGRESS");
        task.setTimerActive(true);

        when(taskAccessService
                .getRequiredForView(1L))
                .thenReturn(task);

        assertThat(taskService.getTimer(1L))
                .isSameAs(task);

        verify(taskAccessService)
                .getRequiredForView(1L);

        verifyNoInteractions(
                taskRepository,
                projectRepository,
                collaboratorRepository,
                currentCollaboratorResolver
        );
    }

    @Test
    void deleteAuthorizesManagementBeforeRepositoryDelete() {
        Task task = new Task();
        task.setId(11L);

        when(taskAccessService
                .getRequiredForManagement(11L))
                .thenReturn(task);

        taskService.delete(11L);

        InOrder order = inOrder(
                taskAccessService,
                taskRepository
        );

        order.verify(taskAccessService)
                .getRequiredForManagement(11L);

        order.verify(taskRepository)
                .delete(task);

        verifyNoInteractions(
                projectRepository,
                collaboratorRepository,
                currentCollaboratorResolver
        );
    }

    @Test
    void completedTaskCannotRestartTimer() {
        Task task = new Task();
        task.setId(1L);
        task.setStatus("COMPLETED");
        task.setTimerActive(false);

        when(taskAccessService
                .getRequiredForAssigneeAction(1L))
                .thenReturn(task);

        assertThatThrownBy(
                () -> taskService.startTimer(1L)
        )
                .isInstanceOf(
                        InvalidOperationException.class
                )
                .hasMessage(
                        "A completed task cannot restart its timer."
                );

        verify(taskAccessService)
                .getRequiredForAssigneeAction(1L);

        verifyNoInteractions(taskRepository);
    }

    @Test
    void resumeTimerUsesAssigneeAuthorization() {
        Task task = new Task();

        task.setId(1L);
        task.setStatus("PENDING");
        task.setTimerActive(false);
        task.setTotalTimeSeconds(0L);

        when(taskAccessService
                .getRequiredForAssigneeAction(1L))
                .thenReturn(task);

        when(taskRepository.save(task))
                .thenReturn(task);

        Task result = taskService.resumeTimer(1L);

        assertThat(result)
                .isSameAs(task);

        assertThat(result.getTimerActive())
                .isTrue();

        assertThat(result.getTimerStartedAt())
                .isEqualTo(
                        LocalDateTime.of(
                                2026,
                                7,
                                14,
                                12,
                                0
                        )
                );

        assertThat(result.getStatus())
                .isEqualTo("IN_PROGRESS");

        InOrder order = inOrder(
                taskAccessService,
                taskRepository
        );

        order.verify(taskAccessService)
                .getRequiredForAssigneeAction(1L);

        order.verify(taskRepository)
                .save(task);
    }

    @Test
    void pauseTimerAddsCurrentSessionToTotal() {
        Task task = new Task();

        task.setId(1L);
        task.setStatus("IN_PROGRESS");
        task.setTimerActive(true);
        task.setTimerStartedAt(
                LocalDateTime.of(
                        2026,
                        7,
                        14,
                        11,
                        58,
                        30
                )
        );
        task.setTotalTimeSeconds(30L);

        when(taskAccessService
                .getRequiredForAssigneeAction(1L))
                .thenReturn(task);

        when(taskRepository.save(task))
                .thenReturn(task);

        Task result = taskService.pauseTimer(1L);

        assertThat(result.getTotalTimeSeconds())
                .isEqualTo(120L);

        assertThat(result.getTimerActive())
                .isFalse();

        assertThat(result.getTimerStartedAt())
                .isNull();

        InOrder order = inOrder(
                taskAccessService,
                taskRepository
        );

        order.verify(taskAccessService)
                .getRequiredForAssigneeAction(1L);

        order.verify(taskRepository)
                .save(task);
    }

    @Test
    void completingTaskStopsTimerAndSetsCompletedStatus() {
        Task task = new Task();

        task.setId(1L);
        task.setStatus("IN_PROGRESS");
        task.setTimerActive(true);
        task.setTimerStartedAt(
                LocalDateTime.of(
                        2026,
                        7,
                        14,
                        11,
                        59,
                        0
                )
        );
        task.setTotalTimeSeconds(10L);

        when(taskAccessService
                .getRequiredForAssigneeAction(1L))
                .thenReturn(task);

        when(taskRepository.save(task))
                .thenReturn(task);

        Task result = taskService.complete(1L);

        assertThat(result.getStatus())
                .isEqualTo("COMPLETED");

        assertThat(result.getTotalTimeSeconds())
                .isEqualTo(70L);

        assertThat(result.getTimerActive())
                .isFalse();

        assertThat(result.getTimerStartedAt())
                .isNull();

        InOrder order = inOrder(
                taskAccessService,
                taskRepository
        );

        order.verify(taskAccessService)
                .getRequiredForAssigneeAction(1L);

        order.verify(taskRepository)
                .save(task);
    }
}
