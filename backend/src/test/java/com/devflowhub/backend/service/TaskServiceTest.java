package com.devflowhub.backend.service;

import com.devflowhub.backend.entity.Task;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import com.devflowhub.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CollaboratorRepository collaboratorRepository;

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
                fixedClock
        );
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
    void completedTaskCannotRestartTimer() {
        Task task = new Task();
        task.setId(1L);
        task.setStatus("COMPLETED");
        task.setTimerActive(false);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.startTimer(1L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("A completed task cannot restart its timer.");
    }

    @Test
    void pauseTimerAddsCurrentSessionToTotal() {
        Task task = new Task();
        task.setId(1L);
        task.setStatus("IN_PROGRESS");
        task.setTimerActive(true);
        task.setTimerStartedAt(LocalDateTime.of(2026, 7, 14, 11, 58, 30));
        task.setTotalTimeSeconds(30L);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.pauseTimer(1L);

        assertThat(result.getTotalTimeSeconds()).isEqualTo(120L);
        assertThat(result.getTimerActive()).isFalse();
        assertThat(result.getTimerStartedAt()).isNull();
        verify(taskRepository).save(task);
    }

    @Test
    void completingTaskStopsTimerAndSetsCompletedStatus() {
        Task task = new Task();
        task.setId(1L);
        task.setStatus("IN_PROGRESS");
        task.setTimerActive(true);
        task.setTimerStartedAt(LocalDateTime.of(2026, 7, 14, 11, 59, 0));
        task.setTotalTimeSeconds(10L);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.complete(1L);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getTotalTimeSeconds()).isEqualTo(70L);
        assertThat(result.getTimerActive()).isFalse();
        assertThat(result.getTimerStartedAt()).isNull();
    }
}
