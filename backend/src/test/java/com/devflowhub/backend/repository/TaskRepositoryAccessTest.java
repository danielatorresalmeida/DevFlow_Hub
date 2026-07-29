package com.devflowhub.backend.repository;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.entity.Project;
import com.devflowhub.backend.entity.ProjectMembership;
import com.devflowhub.backend.entity.Task;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskRepositoryAccessTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMembershipRepository
            membershipRepository;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void accessibleQueryIncludesOnlyActiveProjectAndOwnStandaloneTasks() {
        Collaborator current = createCollaborator(
                "task-access-current@example.com"
        );

        Collaborator other = createCollaborator(
                "task-access-other@example.com"
        );

        Project activeProject = createProject(
                "Active task project"
        );

        Project inactiveProject = createProject(
                "Inactive task project"
        );

        Project hiddenProject = createProject(
                "Hidden task project"
        );

        createMembership(
                activeProject,
                current,
                ProjectMembershipRole.VIEWER,
                ProjectMembershipStatus.ACTIVE
        );

        createMembership(
                inactiveProject,
                current,
                ProjectMembershipRole.CONTRIBUTOR,
                ProjectMembershipStatus.INACTIVE
        );

        createMembership(
                hiddenProject,
                other,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );

        Task activeTaskForOther = createTask(
                "Active project task for other",
                activeProject.getId(),
                other.getId(),
                "PENDING",
                30L
        );

        Task activeUnassignedTask = createTask(
                "Active project unassigned task",
                activeProject.getId(),
                null,
                "REVIEW",
                60L
        );

        Task ownStandaloneTask = createTask(
                "Own standalone task",
                null,
                current.getId(),
                "COMPLETED",
                90L
        );

        createTask(
                "Inactive project task assigned to current",
                inactiveProject.getId(),
                current.getId(),
                "PENDING",
                1000L
        );

        createTask(
                "Hidden project task assigned to current",
                hiddenProject.getId(),
                current.getId(),
                "PENDING",
                2000L
        );

        createTask(
                "Other standalone task",
                null,
                other.getId(),
                "PENDING",
                3000L
        );

        createTask(
                "Unassigned standalone task",
                null,
                null,
                "PENDING",
                4000L
        );

        entityManager.clear();

        List<Task> accessible = taskRepository
                .findAccessibleByCollaboratorIdAndStatus(
                        current.getId(),
                        ProjectMembershipStatus.ACTIVE
                );

        assertThat(accessible)
                .extracting(Task::getId)
                .containsExactly(
                        ownStandaloneTask.getId(),
                        activeUnassignedTask.getId(),
                        activeTaskForOther.getId()
                );

        assertThat(accessible)
                .extracting(Task::getTitle)
                .containsExactly(
                        "Own standalone task",
                        "Active project unassigned task",
                        "Active project task for other"
                );

        assertThat(
                taskRepository
                        .countAccessibleByCollaboratorIdAndStatus(
                                current.getId(),
                                ProjectMembershipStatus.ACTIVE
                        )
        ).isEqualTo(3L);

        assertThat(
                taskRepository
                        .countAccessibleByCollaboratorIdAndStatusAndTaskStatus(
                                current.getId(),
                                ProjectMembershipStatus.ACTIVE,
                                "PENDING"
                        )
        ).isEqualTo(1L);

        assertThat(
                taskRepository
                        .countAccessibleByCollaboratorIdAndStatusAndTaskStatus(
                                current.getId(),
                                ProjectMembershipStatus.ACTIVE,
                                "REVIEW"
                        )
        ).isEqualTo(1L);

        assertThat(
                taskRepository
                        .countAccessibleByCollaboratorIdAndStatusAndTaskStatus(
                                current.getId(),
                                ProjectMembershipStatus.ACTIVE,
                                "COMPLETED"
                        )
        ).isEqualTo(1L);

        assertThat(
                taskRepository
                        .countAccessibleByCollaboratorIdAndStatusAndTaskStatus(
                                current.getId(),
                                ProjectMembershipStatus.ACTIVE,
                                "IN_PROGRESS"
                        )
        ).isZero();

        assertThat(
                taskRepository
                        .sumAccessibleStoredTimeSecondsByCollaboratorIdAndStatus(
                                current.getId(),
                                ProjectMembershipStatus.ACTIVE
                        )
        ).isEqualTo(180L);
    }

    private Collaborator createCollaborator(
            String email
    ) {
        Collaborator collaborator =
                new Collaborator();

        collaborator.setName(
                "Task Access Tester"
        );

        collaborator.setEmail(email);

        collaborator.setPassword(
                "{bcrypt}test-password-hash"
        );

        collaborator.setRole("Developer");
        collaborator.setActive(true);

        return collaboratorRepository
                .saveAndFlush(collaborator);
    }

    private Project createProject(
            String name
    ) {
        Project project = new Project();

        project.setName(name);
        project.setStatus("PLANNED");

        return projectRepository
                .saveAndFlush(project);
    }

    private void createMembership(
            Project project,
            Collaborator collaborator,
            ProjectMembershipRole role,
            ProjectMembershipStatus status
    ) {
        ProjectMembership membership =
                new ProjectMembership();

        membership.setProjectId(
                project.getId()
        );

        membership.setCollaboratorId(
                collaborator.getId()
        );

        membership.setRole(role);
        membership.setStatus(status);

        membershipRepository
                .saveAndFlush(membership);
    }

    private Task createTask(
            String title,
            Long projectId,
            Long assigneeId,
            String status,
            Long totalTimeSeconds
    ) {
        Task task = new Task();

        task.setTitle(title);
        task.setStatus(status);
        task.setPriority("MEDIUM");
        task.setProjectId(projectId);
        task.setAssigneeId(assigneeId);
        task.setTotalTimeSeconds(totalTimeSeconds);

        return taskRepository
                .saveAndFlush(task);
    }
}
