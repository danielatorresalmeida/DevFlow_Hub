package com.devflowhub.backend.repository;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.entity.Project;
import com.devflowhub.backend.entity.ProjectMembership;
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
class ProjectRepositoryAccessTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMembershipRepository membershipRepository;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Test
    void accessibleQueriesIncludeOnlyActiveMembershipProjects() {
        Collaborator current = createCollaborator(
                "project-access-current@example.com"
        );
        Collaborator other = createCollaborator(
                "project-access-other@example.com"
        );

        Project beta = createProject("Beta");
        Project alpha = createProject("Alpha");
        Project inactive = createProject("Inactive");
        Project hidden = createProject("Hidden");

        createMembership(
                alpha,
                current,
                ProjectMembershipRole.VIEWER,
                ProjectMembershipStatus.ACTIVE
        );
        createMembership(
                beta,
                current,
                ProjectMembershipRole.CONTRIBUTOR,
                ProjectMembershipStatus.ACTIVE
        );
        createMembership(
                inactive,
                current,
                ProjectMembershipRole.MANAGER,
                ProjectMembershipStatus.INACTIVE
        );
        createMembership(
                hidden,
                other,
                ProjectMembershipRole.OWNER,
                ProjectMembershipStatus.ACTIVE
        );

        List<Project> accessible = projectRepository
                .findAccessibleByCollaboratorIdAndStatus(
                        current.getId(),
                        ProjectMembershipStatus.ACTIVE
                );

        assertThat(accessible)
                .extracting(Project::getName)
                .containsExactly("Alpha", "Beta");

        assertThat(projectRepository
                .countAccessibleByCollaboratorIdAndStatus(
                        current.getId(),
                        ProjectMembershipStatus.ACTIVE
                ))
                .isEqualTo(2L);
    }

    private Collaborator createCollaborator(String email) {
        Collaborator collaborator = new Collaborator();
        collaborator.setName("Project Access Tester");
        collaborator.setEmail(email);
        collaborator.setPassword("{bcrypt}test-password-hash");
        collaborator.setRole("Developer");
        collaborator.setActive(true);
        return collaboratorRepository.saveAndFlush(collaborator);
    }

    private Project createProject(String name) {
        Project project = new Project();
        project.setName(name);
        project.setStatus("PLANNED");
        return projectRepository.saveAndFlush(project);
    }

    private void createMembership(
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
        membershipRepository.saveAndFlush(membership);
    }
}
