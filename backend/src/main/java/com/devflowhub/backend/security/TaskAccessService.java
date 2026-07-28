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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class TaskAccessService {

    private static final String HIDDEN_TASK_MESSAGE =
            "Task not found.";

    private final TaskRepository taskRepository;
    private final CurrentCollaboratorResolver
            currentCollaboratorResolver;
    private final ProjectAccessService projectAccessService;
    private final ProjectMembershipRepository
            projectMembershipRepository;

    public TaskAccessService(
            TaskRepository taskRepository,
            CurrentCollaboratorResolver
                    currentCollaboratorResolver,
            ProjectAccessService projectAccessService,
            ProjectMembershipRepository
                    projectMembershipRepository
    ) {
        this.taskRepository = taskRepository;
        this.currentCollaboratorResolver =
                currentCollaboratorResolver;
        this.projectAccessService = projectAccessService;
        this.projectMembershipRepository =
                projectMembershipRepository;
    }

    public void prepareForCreate(Task task) {
        prepareDestination(task);
    }

    public Task getRequiredAndPrepareForUpdate(
            Long taskId,
            Task updatedData
    ) {
        Task existing = getStoredTaskRequired(taskId);

        requireManagement(existing);
        prepareDestination(updatedData);

        return existing;
    }

    private void prepareDestination(Task task) {
        if (task.getProjectId() == null) {
            prepareStandaloneCreate(task);
            return;
        }

        ProjectMembership callerMembership =
                projectAccessService
                        .requirePermission(
                                task.getProjectId(),
                                ProjectPermission
                                        .CONTRIBUTE_TO_PROJECT
                        );

        Long callerId =
                callerMembership.getCollaboratorId();

        Long assigneeId = task.getAssigneeId();

        if (assigneeId == null) {
            if (
                callerMembership.getRole() ==
                        ProjectMembershipRole.CONTRIBUTOR
            ) {
                task.setAssigneeId(callerId);
            }

            return;
        }

        if (Objects.equals(
                assigneeId,
                callerId
        )) {
            return;
        }

        if (
            callerMembership.getRole() ==
                    ProjectMembershipRole.CONTRIBUTOR
        ) {
            throw new ProjectAccessDeniedException();
        }

        boolean activeAssignee =
                projectMembershipRepository
                        .existsByProjectIdAndCollaboratorIdAndStatus(
                                task.getProjectId(),
                                assigneeId,
                                ProjectMembershipStatus.ACTIVE
                        );

        if (!activeAssignee) {
            throw new InvalidOperationException(
                    "The selected assignee must be " +
                    "an active project member."
            );
        }
    }

    public Task getRequiredForView(Long taskId) {
        Task task = getStoredTaskRequired(taskId);

        requireView(task);

        return task;
    }

    public Task getRequiredForManagement(Long taskId) {
        Task task = getStoredTaskRequired(taskId);

        requireManagement(task);

        return task;
    }

    public Task getRequiredForAssigneeAction(Long taskId) {
        Task task = getStoredTaskRequired(taskId);

        requireAssigneeAction(task);

        return task;
    }

    private Task getStoredTaskRequired(Long taskId) {
        if (taskId == null) {
            throw hiddenTask();
        }

        return taskRepository.findById(taskId)
                .orElseThrow(this::hiddenTask);
    }

    private void requireView(Task task) {
        if (task.getProjectId() != null) {
            requireVisibleProjectMembership(
                    task.getProjectId()
            );

            return;
        }

        requireStandaloneAssignee(task);
    }

    private void requireManagement(Task task) {
        if (task.getProjectId() == null) {
            requireStandaloneAssignee(task);
            return;
        }

        ProjectMembership membership =
                requireVisibleProjectMembership(
                        task.getProjectId()
                );

        ProjectMembershipRole role =
                membership.getRole();

        if (
            role == ProjectMembershipRole.OWNER ||
            role == ProjectMembershipRole.MANAGER
        ) {
            return;
        }

        if (
            role == ProjectMembershipRole.CONTRIBUTOR &&
            Objects.equals(
                    task.getAssigneeId(),
                    membership.getCollaboratorId()
            )
        ) {
            return;
        }

        throw new ProjectAccessDeniedException();
    }

    private void requireAssigneeAction(Task task) {
        if (task.getProjectId() == null) {
            requireStandaloneAssignee(task);
            return;
        }

        ProjectMembership membership =
                requireVisibleProjectMembership(
                        task.getProjectId()
                );

        if (!Objects.equals(
                task.getAssigneeId(),
                membership.getCollaboratorId()
        )) {
            throw new ProjectAccessDeniedException();
        }
    }

    private ProjectMembership
            requireVisibleProjectMembership(
                    Long projectId
            ) {
        try {
            return projectAccessService
                    .requirePermission(
                            projectId,
                            ProjectPermission.VIEW_PROJECT
                    );
        }
        catch (ResourceNotFoundException exception) {
            throw hiddenTask();
        }
    }

    private void prepareStandaloneCreate(Task task) {
        Long collaboratorId =
                currentCollaboratorResolver
                        .getRequiredId();

        if (task.getAssigneeId() == null) {
            task.setAssigneeId(collaboratorId);
            return;
        }

        if (!Objects.equals(
                task.getAssigneeId(),
                collaboratorId
        )) {
            throw new InvalidOperationException(
                    "A standalone task must be assigned " +
                    "to the authenticated collaborator."
            );
        }
    }

    private void requireStandaloneAssignee(Task task) {
        Long collaboratorId =
                currentCollaboratorResolver
                        .getRequiredId();

        if (!Objects.equals(
                task.getAssigneeId(),
                collaboratorId
        )) {
            throw hiddenTask();
        }
    }

    private ResourceNotFoundException hiddenTask() {
        return new ResourceNotFoundException(
                HIDDEN_TASK_MESSAGE
        );
    }
}
