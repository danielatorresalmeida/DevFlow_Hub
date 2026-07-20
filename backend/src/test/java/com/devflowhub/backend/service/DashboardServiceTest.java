package com.devflowhub.backend.service;

import com.devflowhub.backend.dto.DashboardSummary;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.entity.Project;
import com.devflowhub.backend.entity.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private CollaboratorService collaboratorService;

    @Mock
    private ProjectService projectService;

    @Mock
    private TaskService taskService;

    @Mock
    private InternalProgramService internalProgramService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-16T10:00:00Z"), ZoneOffset.UTC);
        dashboardService = new DashboardService(
                collaboratorService,
                projectService,
                taskService,
                internalProgramService,
                clock
        );
    }

    @Test
    void summaryContainsCountsRecentTasksAndUpcomingProjects() {
        Collaborator collaborator = new Collaborator();
        collaborator.setId(1L);
        collaborator.setName("Ana");

        Task task = new Task();
        task.setId(3L);
        task.setTitle("Review dashboard");
        task.setStatus("REVIEW");
        task.setPriority("HIGH");
        task.setAssigneeId(1L);
        task.setUpdatedAt(LocalDateTime.of(2026, 7, 16, 9, 0));

        Project project = new Project();
        project.setId(7L);
        project.setName("DevFlow Hub");
        project.setStatus("IN_PROGRESS");
        project.setEndDate(LocalDate.of(2026, 7, 30));
        project.setManagerId(1L);

        when(collaboratorService.findAll()).thenReturn(List.of(collaborator));
        when(projectService.findAll()).thenReturn(List.of(project));
        when(taskService.findAll()).thenReturn(List.of(task));
        when(internalProgramService.count()).thenReturn(2L);
        when(taskService.getStoredTimeSeconds()).thenReturn(3600L);

        DashboardSummary summary = dashboardService.getSummary();

        assertThat(summary.collaboratorCount()).isEqualTo(1);
        assertThat(summary.projectCount()).isEqualTo(1);
        assertThat(summary.taskCount()).isEqualTo(1);
        assertThat(summary.programCount()).isEqualTo(2);
        assertThat(summary.reviewTaskCount()).isEqualTo(1);
        assertThat(summary.trackedTimeSeconds()).isEqualTo(3600);
        assertThat(summary.recentTasks()).singleElement()
                .satisfies(item -> assertThat(item.assigneeName()).isEqualTo("Ana"));
        assertThat(summary.upcomingProjects()).singleElement()
                .satisfies(item -> assertThat(item.managerName()).isEqualTo("Ana"));
    }
}
