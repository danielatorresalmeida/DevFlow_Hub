package com.devflowhub.backend.security;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.entity.Project;
import com.devflowhub.backend.entity.ProjectMembership;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.ProjectMembershipRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import com.devflowhub.backend.service.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProjectOwnershipTransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMembershipRepository projectMembershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void ownerTransfersOwnershipAndManagerReferenceTogether()
            throws Exception {
        Collaborator currentOwner = saveCollaborator(
                "Current Owner",
                true
        );
        Collaborator newOwner = saveCollaborator(
                "New Owner",
                true
        );
        Project project = saveProject("Ownership project", currentOwner);
        ProjectMembership currentOwnerMembership = saveMembership(
                project,
                currentOwner,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );
        ProjectMembership newOwnerMembership = saveMembership(
                project,
                newOwner,
                ProjectMembershipRole.MANAGER,
                ProjectMembershipStatus.ACTIVE
        );

        mockMvc.perform(post(
                        "/api/projects/{projectId}/ownership-transfer",
                        project.getId()
                )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(currentOwner)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(
                                newOwner.getId(),
                                currentOwnerMembership.getVersion(),
                                newOwnerMembership.getVersion()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managerId")
                        .value(newOwner.getId().intValue()))
                .andExpect(jsonPath("$.previousOwner.role")
                        .value("MANAGER"))
                .andExpect(jsonPath("$.newOwner.role")
                        .value("OWNER"));

        assertOwnership(
                project,
                currentOwner,
                newOwner,
                ProjectMembershipRole.MANAGER,
                ProjectMembershipRole.OWNER,
                newOwner.getId()
        );
    }

    @Test
    void managerCannotTransferOwnershipAndNothingChanges()
            throws Exception {
        Collaborator owner = saveCollaborator("Owner", true);
        Collaborator caller = saveCollaborator("Manager", true);
        Collaborator target = saveCollaborator("Target", true);
        Project project = saveProject("Forbidden transfer", owner);
        saveMembership(
                project,
                owner,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );
        ProjectMembership callerMembership = saveMembership(
                project,
                caller,
                ProjectMembershipRole.MANAGER,
                ProjectMembershipStatus.ACTIVE
        );
        ProjectMembership targetMembership = saveMembership(
                project,
                target,
                ProjectMembershipRole.CONTRIBUTOR,
                ProjectMembershipStatus.ACTIVE
        );

        mockMvc.perform(post(
                        "/api/projects/{projectId}/ownership-transfer",
                        project.getId()
                )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(caller)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(
                                target.getId(),
                                callerMembership.getVersion(),
                                targetMembership.getVersion()
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "You do not have permission to perform this operation."
                ));

        assertOwnership(
                project,
                owner,
                target,
                ProjectMembershipRole.OWNER,
                ProjectMembershipRole.CONTRIBUTOR,
                owner.getId()
        );
    }

    @Test
    void inactiveCollaboratorCannotReceiveOwnership()
            throws Exception {
        Collaborator owner = saveCollaborator("Owner", true);
        Collaborator target = saveCollaborator("Inactive Target", false);
        Project project = saveProject("Inactive target", owner);
        ProjectMembership ownerMembership = saveMembership(
                project,
                owner,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );
        ProjectMembership targetMembership = saveMembership(
                project,
                target,
                ProjectMembershipRole.MANAGER,
                ProjectMembershipStatus.ACTIVE
        );

        mockMvc.perform(post(
                        "/api/projects/{projectId}/ownership-transfer",
                        project.getId()
                )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(owner)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(
                                target.getId(),
                                ownerMembership.getVersion(),
                                targetMembership.getVersion()
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "The new owner collaborator must be active."
                ));

        assertOwnership(
                project,
                owner,
                target,
                ProjectMembershipRole.OWNER,
                ProjectMembershipRole.MANAGER,
                owner.getId()
        );
    }

    @Test
    void staleTargetVersionReturnsConflictWithoutPartialUpdate()
            throws Exception {
        Collaborator owner = saveCollaborator("Owner", true);
        Collaborator target = saveCollaborator("Target", true);
        Project project = saveProject("Stale transfer", owner);
        ProjectMembership ownerMembership = saveMembership(
                project,
                owner,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );
        ProjectMembership targetMembership = saveMembership(
                project,
                target,
                ProjectMembershipRole.VIEWER,
                ProjectMembershipStatus.ACTIVE
        );

        mockMvc.perform(post(
                        "/api/projects/{projectId}/ownership-transfer",
                        project.getId()
                )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearerToken(owner)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(
                                target.getId(),
                                ownerMembership.getVersion(),
                                targetMembership.getVersion() + 1L
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "New owner membership was modified by another request. Refresh and try again."
                ));

        assertOwnership(
                project,
                owner,
                target,
                ProjectMembershipRole.OWNER,
                ProjectMembershipRole.VIEWER,
                owner.getId()
        );
    }

    private Collaborator saveCollaborator(
            String name,
            boolean active
    ) {
        Collaborator collaborator = new Collaborator();
        collaborator.setName(name);
        collaborator.setEmail(
                "ownership-" + UUID.randomUUID() + "@example.com"
        );
        collaborator.setPassword(
                passwordEncoder.encode("test-password")
        );
        collaborator.setRole("Developer");
        collaborator.setActive(active);
        return collaboratorRepository.saveAndFlush(collaborator);
    }

    private Project saveProject(
            String name,
            Collaborator manager
    ) {
        Project project = new Project();
        project.setName(name);
        project.setStatus("ACTIVE");
        project.setManagerId(manager.getId());
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
        return projectMembershipRepository.saveAndFlush(membership);
    }

    private String bearerToken(Collaborator collaborator) {
        return "Bearer " + jwtTokenService
                .issue(collaborator)
                .accessToken();
    }

    private String requestBody(
            Long newOwnerCollaboratorId,
            Long currentOwnerVersion,
            Long newOwnerVersion
    ) {
        return """
                {
                  "newOwnerCollaboratorId": %d,
                  "currentOwnerMembershipVersion": %d,
                  "newOwnerMembershipVersion": %d
                }
                """.formatted(
                        newOwnerCollaboratorId,
                        currentOwnerVersion,
                        newOwnerVersion
                );
    }

    private void assertOwnership(
            Project project,
            Collaborator previousOwner,
            Collaborator target,
            ProjectMembershipRole previousOwnerRole,
            ProjectMembershipRole targetRole,
            Long expectedManagerId
    ) {
        Project persistedProject = projectRepository
                .findById(project.getId())
                .orElseThrow();
        ProjectMembership persistedPreviousOwner =
                projectMembershipRepository
                        .findByProjectIdAndCollaboratorId(
                                project.getId(),
                                previousOwner.getId()
                        )
                        .orElseThrow();
        ProjectMembership persistedTarget =
                projectMembershipRepository
                        .findByProjectIdAndCollaboratorId(
                                project.getId(),
                                target.getId()
                        )
                        .orElseThrow();

        assertThat(persistedPreviousOwner.getRole())
                .isEqualTo(previousOwnerRole);
        assertThat(persistedTarget.getRole())
                .isEqualTo(targetRole);
        assertThat(persistedProject.getManagerId())
                .isEqualTo(expectedManagerId);
    }
}
