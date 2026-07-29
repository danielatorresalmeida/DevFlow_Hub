package com.devflowhub.backend.service;

import com.devflowhub.backend.domain.DomainValues;
import com.devflowhub.backend.dto.DashboardProjectItem;
import com.devflowhub.backend.dto.DashboardSummary;
import com.devflowhub.backend.dto.DashboardTaskItem;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.entity.Project;
import com.devflowhub.backend.entity.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final CollaboratorService collaboratorService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final InternalProgramService internalProgramService;
    private final Clock clock;

    public DashboardService(
            CollaboratorService collaboratorService,
            ProjectService projectService,
            TaskService taskService,
            InternalProgramService internalProgramService,
            Clock clock
    ) {
        this.collaboratorService = collaboratorService;
        this.projectService = projectService;
        this.taskService = taskService;
        this.internalProgramService = internalProgramService;
        this.clock = clock;
    }

    public DashboardSummary getSummary() {
        List<Collaborator> collaborators = collaboratorService.findAll();
        List<Project> projects = projectService.findAll();
        List<Task> tasks = taskService.findAll();
        Map<Long, Collaborator> collaboratorById = collaborators.stream()
                .filter(collaborator -> collaborator.getId() != null)
                .collect(Collectors.toMap(Collaborator::getId, Function.identity()));

        return new DashboardSummary(
                collaborators.size(),
                projects.size(),
                tasks.size(),
                internalProgramService.count(),
                countTasksByStatus(tasks, DomainValues.TaskStatus.PENDING),
                countTasksByStatus(tasks, DomainValues.TaskStatus.IN_PROGRESS),
                countTasksByStatus(tasks, DomainValues.TaskStatus.REVIEW),
                countTasksByStatus(tasks, DomainValues.TaskStatus.COMPLETED),
                taskService.getStoredTimeSeconds(),
                buildRecentTasks(tasks, collaboratorById),
                buildUpcomingProjects(projects, collaboratorById)
        );
    }

    private long countTasksByStatus(List<Task> tasks, String status) {
        return tasks.stream()
                .filter(task -> status.equalsIgnoreCase(task.getStatus()))
                .count();
    }

    private List<DashboardTaskItem> buildRecentTasks(
            List<Task> tasks,
            Map<Long, Collaborator> collaboratorById
    ) {
        return tasks.stream()
                .sorted(Comparator.comparing(
                        this::getLastActivity,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(4)
                .map(task -> new DashboardTaskItem(
                        task.getId(),
                        task.getTitle(),
                        task.getStatus(),
                        task.getPriority(),
                        getLastActivity(task),
                        getCollaboratorName(task.getAssigneeId(), collaboratorById)
                ))
                .toList();
    }

    private List<DashboardProjectItem> buildUpcomingProjects(
            List<Project> projects,
            Map<Long, Collaborator> collaboratorById
    ) {
        LocalDate today = LocalDate.now(clock);

        return projects.stream()
                .filter(project -> project.getEndDate() != null)
                .filter(project -> !project.getEndDate().isBefore(today))
                .filter(project -> !DomainValues.ProjectStatus.COMPLETED.equalsIgnoreCase(project.getStatus()))
                .sorted(Comparator.comparing(Project::getEndDate))
                .limit(4)
                .map(project -> new DashboardProjectItem(
                        project.getId(),
                        project.getName(),
                        project.getStatus(),
                        project.getEndDate(),
                        getCollaboratorName(project.getManagerId(), collaboratorById)
                ))
                .toList();
    }

    private LocalDateTime getLastActivity(Task task) {
        return task.getUpdatedAt() != null ? task.getUpdatedAt() : task.getCreatedAt();
    }

    private String getCollaboratorName(
            Long collaboratorId,
            Map<Long, Collaborator> collaboratorById
    ) {
        if (collaboratorId == null) {
            return "Not assigned";
        }

        Collaborator collaborator = collaboratorById.get(collaboratorId);
        return collaborator == null ? "Not assigned" : collaborator.getName();
    }
}
