package com.devflowhub.backend.security;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.entity.Project;
import com.devflowhub.backend.entity.ProjectMembership;
import com.devflowhub.backend.entity.Task;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.ProjectMembershipRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import com.devflowhub.backend.repository.TaskRepository;
import com.devflowhub.backend.service.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskMoveAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMembershipRepository
            projectMembershipRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void movingTaskFromHiddenCurrentProjectReturnsNotFound()
            throws Exception {
        Collaborator caller = saveActiveCollaborator("Caller");
        Collaborator assignee = saveActiveCollaborator("Assignee");
        Project currentProject = saveProject("Hidden current");
        Project destinationProject = saveProject("Visible destination");

        saveMembership(
                destinationProject,
                caller,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );
        saveMembership(
                destinationProject,
                assignee,
                ProjectMembershipRole.CONTRIBUTOR,
                ProjectMembershipStatus.ACTIVE
        );

        Task task = saveTask(currentProject, assignee);

        mockMvc.perform(put("/api/tasks/{id}", task.getId())
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(caller)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(
                                destinationProject.getId(),
                                assignee.getId()
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Task not found."));

        assertTaskLocation(
                task.getId(),
                currentProject.getId(),
                assignee.getId()
        );
    }

    @Test
    void currentProjectAuthorizationRunsBeforeDestinationValidation()
            throws Exception {
        Collaborator caller = saveActiveCollaborator("Viewer");
        Collaborator assignee = saveActiveCollaborator("Assignee");
        Project currentProject = saveProject("Current");

        saveMembership(
                currentProject,
                caller,
                ProjectMembershipRole.VIEWER,
                ProjectMembershipStatus.ACTIVE
        );

        Task task = saveTask(currentProject, assignee);
        long missingDestinationId = Long.MAX_VALUE;

        mockMvc.perform(put("/api/tasks/{id}", task.getId())
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(caller)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(
                                missingDestinationId,
                                assignee.getId()
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "You do not have permission to " +
                        "perform this operation."
                ));

        assertTaskLocation(
                task.getId(),
                currentProject.getId(),
                assignee.getId()
        );
    }

    @Test
    void movingTaskToMissingDestinationReturnsNotFound()
            throws Exception {
        Collaborator caller = saveActiveCollaborator("Owner");
        Collaborator assignee = saveActiveCollaborator("Assignee");
        Project currentProject = saveProject("Current");

        saveMembership(
                currentProject,
                caller,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );

        Task task = saveTask(currentProject, assignee);
        long missingDestinationId = Long.MAX_VALUE;

        mockMvc.perform(put("/api/tasks/{id}", task.getId())
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(caller)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(
                                missingDestinationId,
                                assignee.getId()
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Project not found."));

        assertTaskLocation(
                task.getId(),
                currentProject.getId(),
                assignee.getId()
        );
    }

    @Test
    void movingTaskToInvisibleDestinationReturnsNotFound()
            throws Exception {
        Collaborator caller = saveActiveCollaborator("Owner");
        Collaborator assignee = saveActiveCollaborator("Assignee");
        Project currentProject = saveProject("Current");
        Project destinationProject = saveProject("Hidden destination");

        saveMembership(
                currentProject,
                caller,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );

        Task task = saveTask(currentProject, assignee);

        mockMvc.perform(put("/api/tasks/{id}", task.getId())
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(caller)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(
                                destinationProject.getId(),
                                assignee.getId()
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Project not found."));

        assertTaskLocation(
                task.getId(),
                currentProject.getId(),
                assignee.getId()
        );
    }

    @Test
    void movingTaskToViewOnlyDestinationReturnsForbidden()
            throws Exception {
        Collaborator caller = saveActiveCollaborator("Owner and viewer");
        Collaborator assignee = saveActiveCollaborator("Assignee");
        Project currentProject = saveProject("Current");
        Project destinationProject = saveProject("View only destination");

        saveMembership(
                currentProject,
                caller,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );
        saveMembership(
                destinationProject,
                caller,
                ProjectMembershipRole.VIEWER,
                ProjectMembershipStatus.ACTIVE
        );

        Task task = saveTask(currentProject, assignee);

        mockMvc.perform(put("/api/tasks/{id}", task.getId())
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(caller)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(
                                destinationProject.getId(),
                                assignee.getId()
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "You do not have permission to " +
                        "perform this operation."
                ));

        assertTaskLocation(
                task.getId(),
                currentProject.getId(),
                assignee.getId()
        );
    }

    @Test
    void movingTaskRejectsAssigneeOutsideDestinationProject()
            throws Exception {
        Collaborator caller = saveActiveCollaborator("Owner");
        Collaborator assignee = saveActiveCollaborator("Assignee");
        Project currentProject = saveProject("Current");
        Project destinationProject = saveProject("Destination");

        saveMembership(
                currentProject,
                caller,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );
        saveMembership(
                destinationProject,
                caller,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );

        Task task = saveTask(currentProject, assignee);

        mockMvc.perform(put("/api/tasks/{id}", task.getId())
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(caller)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(
                                destinationProject.getId(),
                                assignee.getId()
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "The selected assignee must be an " +
                        "active project member."
                ));

        assertTaskLocation(
                task.getId(),
                currentProject.getId(),
                assignee.getId()
        );
    }

    @Test
    void movingTaskRejectsInactiveDestinationAssignee()
            throws Exception {
        Collaborator caller = saveActiveCollaborator("Owner");
        Collaborator assignee = saveActiveCollaborator("Assignee");
        Project currentProject = saveProject("Current");
        Project destinationProject = saveProject("Destination");

        saveMembership(
                currentProject,
                caller,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );
        saveMembership(
                destinationProject,
                caller,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );
        saveMembership(
                destinationProject,
                assignee,
                ProjectMembershipRole.CONTRIBUTOR,
                ProjectMembershipStatus.INACTIVE
        );

        Task task = saveTask(currentProject, assignee);

        mockMvc.perform(put("/api/tasks/{id}", task.getId())
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(caller)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(
                                destinationProject.getId(),
                                assignee.getId()
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "The selected assignee must be an " +
                        "active project member."
                ));

        assertTaskLocation(
                task.getId(),
                currentProject.getId(),
                assignee.getId()
        );
    }

    @Test
    void ownerCanMoveTaskToAuthorizedDestination()
            throws Exception {
        Collaborator caller = saveActiveCollaborator("Owner");
        Collaborator assignee = saveActiveCollaborator("Assignee");
        Project currentProject = saveProject("Current");
        Project destinationProject = saveProject("Destination");

        saveMembership(
                currentProject,
                caller,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );
        saveMembership(
                destinationProject,
                caller,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );
        saveMembership(
                destinationProject,
                assignee,
                ProjectMembershipRole.CONTRIBUTOR,
                ProjectMembershipStatus.ACTIVE
        );

        Task task = saveTask(currentProject, assignee);

        mockMvc.perform(put("/api/tasks/{id}", task.getId())
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(caller)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(
                                destinationProject.getId(),
                                assignee.getId()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId")
                        .value(destinationProject.getId().intValue()))
                .andExpect(jsonPath("$.assigneeId")
                        .value(assignee.getId().intValue()));

        assertTaskLocation(
                task.getId(),
                destinationProject.getId(),
                assignee.getId()
        );
    }

    @Test
    void ownerCanMoveTaskWithoutAssignee()
            throws Exception {
        Collaborator caller = saveActiveCollaborator("Owner");
        Collaborator assignee = saveActiveCollaborator("Assignee");
        Project currentProject = saveProject("Current");
        Project destinationProject = saveProject("Destination");

        saveMembership(
                currentProject,
                caller,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );
        saveMembership(
                destinationProject,
                caller,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );

        Task task = saveTask(currentProject, assignee);

        mockMvc.perform(put("/api/tasks/{id}", task.getId())
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(caller)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(
                                destinationProject.getId(),
                                null
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId")
                        .value(destinationProject.getId().intValue()))
                .andExpect(jsonPath("$.assigneeId")
                        .value(org.hamcrest.Matchers.nullValue()));

        assertTaskLocation(
                task.getId(),
                destinationProject.getId(),
                null
        );
    }

    @Test
    void contributorMoveWithoutAssigneeAssignsCaller()
            throws Exception {
        Collaborator caller = saveActiveCollaborator("Contributor");
        Project currentProject = saveProject("Current");
        Project destinationProject = saveProject("Destination");

        saveMembership(
                currentProject,
                caller,
                ProjectMembershipRole.CONTRIBUTOR,
                ProjectMembershipStatus.ACTIVE
        );
        saveMembership(
                destinationProject,
                caller,
                ProjectMembershipRole.CONTRIBUTOR,
                ProjectMembershipStatus.ACTIVE
        );

        Task task = saveTask(currentProject, caller);

        mockMvc.perform(put("/api/tasks/{id}", task.getId())
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(caller)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(
                                destinationProject.getId(),
                                null
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId")
                        .value(destinationProject.getId().intValue()))
                .andExpect(jsonPath("$.assigneeId")
                        .value(caller.getId().intValue()));

        assertTaskLocation(
                task.getId(),
                destinationProject.getId(),
                caller.getId()
        );
    }

    private Collaborator saveActiveCollaborator(String name) {
        Collaborator collaborator = new Collaborator();
        collaborator.setName(name);
        collaborator.setEmail(
                "task-move-" + UUID.randomUUID() +
                "@example.com"
        );
        collaborator.setPassword(
                passwordEncoder.encode("test-password")
        );
        collaborator.setRole("Developer");
        collaborator.setActive(true);

        return collaboratorRepository.saveAndFlush(collaborator);
    }

    private Project saveProject(String name) {
        Project project = new Project();
        project.setName(name);
        project.setStatus("ACTIVE");

        return projectRepository.saveAndFlush(project);
    }

    private ProjectMembership saveMembership(
            Project project,
            Collaborator collaborator,
            ProjectMembershipRole role,
            ProjectMembershipStatus status
    ) {
        ProjectMembership membership = new ProjectMembership();
        membership.setProjectId(project.getId());
        membership.setCollaboratorId(collaborator.getId());
        membership.setRole(role);
        membership.setStatus(status);

        return projectMembershipRepository
                .saveAndFlush(membership);
    }

    private Task saveTask(
            Project project,
            Collaborator assignee
    ) {
        Task task = new Task();
        task.setTitle("Task to move");
        task.setDescription("Original description");
        task.setStatus("PENDING");
        task.setPriority("MEDIUM");
        task.setProjectId(project.getId());
        task.setAssigneeId(assignee.getId());

        return taskRepository.saveAndFlush(task);
    }

    private String bearerToken(Collaborator collaborator) {
        return "Bearer " + jwtTokenService
                .issue(collaborator)
                .accessToken();
    }

    private String updateBody(
            Long projectId,
            Long assigneeId
    ) {
        String assigneeJson = assigneeId == null
                ? "null"
                : assigneeId.toString();

        return """
                {
                  "title": "Moved task",
                  "description": "Updated description",
                  "status": "PENDING",
                  "priority": "MEDIUM",
                  "projectId": %d,
                  "assigneeId": %s
                }
                """.formatted(projectId, assigneeJson);
    }

    private void assertTaskLocation(
            Long taskId,
            Long expectedProjectId,
            Long expectedAssigneeId
    ) {
        Task persisted = taskRepository
                .findById(taskId)
                .orElseThrow();

        assertThat(persisted.getProjectId())
                .isEqualTo(expectedProjectId);
        assertThat(persisted.getAssigneeId())
                .isEqualTo(expectedAssigneeId);
    }
}
