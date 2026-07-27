package com.devflowhub.backend.entity;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.ProjectMembershipRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectMembershipPersistenceTest {

    @Autowired
    private ProjectMembershipRepository membershipRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsMembershipWithRoleStatusAndAuditFields() {
        Collaborator collaborator =
                createCollaborator(
                        "membership-owner@example.com"
                );

        Project project =
                createProject(collaborator.getId());

        ProjectMembership membership =
                createMembership(
                        project.getId(),
                        collaborator.getId(),
                        ProjectMembershipRole.OWNER
                );

        ProjectMembership saved =
                membershipRepository.saveAndFlush(
                        membership
                );

        Long membershipId = saved.getId();

        entityManager.clear();

        ProjectMembership persisted =
                membershipRepository
                        .findById(membershipId)
                        .orElseThrow();

        assertThat(persisted.getProjectId())
                .isEqualTo(project.getId());

        assertThat(persisted.getCollaboratorId())
                .isEqualTo(collaborator.getId());

        assertThat(persisted.getRole())
                .isEqualTo(
                        ProjectMembershipRole.OWNER
                );

        assertThat(persisted.getStatus())
                .isEqualTo(
                        ProjectMembershipStatus.ACTIVE
                );

        assertThat(persisted.getVersion())
                .isNotNull()
                .isGreaterThanOrEqualTo(0L);

        assertThat(persisted.getCreatedAt())
                .isNotNull();

        assertThat(persisted.getUpdatedAt())
                .isEqualTo(persisted.getCreatedAt());
    }

    @Test
    void repositoryFindsActiveMembershipByProjectAndCollaborator() {
        Collaborator collaborator =
                createCollaborator(
                        "membership-contributor@example.com"
                );

        Project project =
                createProject(null);

        membershipRepository.saveAndFlush(
                createMembership(
                        project.getId(),
                        collaborator.getId(),
                        ProjectMembershipRole.CONTRIBUTOR
                )
        );

        assertThat(
                membershipRepository
                        .findByProjectIdAndCollaboratorId(
                                project.getId(),
                                collaborator.getId()
                        )
        ).isPresent();

        assertThat(
                membershipRepository
                        .existsByProjectIdAndCollaboratorIdAndStatus(
                                project.getId(),
                                collaborator.getId(),
                                ProjectMembershipStatus.ACTIVE
                        )
        ).isTrue();

        assertThat(
                membershipRepository
                        .findByProjectIdAndStatusOrderByCreatedAtAsc(
                                project.getId(),
                                ProjectMembershipStatus.ACTIVE
                        )
        ).hasSize(1);
    }

    @Test
    void preventsDuplicateMembershipForTheSameProjectAndCollaborator() {
        Collaborator collaborator =
                createCollaborator(
                        "membership-duplicate@example.com"
                );

        Project project =
                createProject(null);

        membershipRepository.saveAndFlush(
                createMembership(
                        project.getId(),
                        collaborator.getId(),
                        ProjectMembershipRole.CONTRIBUTOR
                )
        );

        ProjectMembership duplicate =
                createMembership(
                        project.getId(),
                        collaborator.getId(),
                        ProjectMembershipRole.VIEWER
                );

        assertThatThrownBy(
                () -> membershipRepository
                        .saveAndFlush(duplicate)
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    private Collaborator createCollaborator(
            String email
    ) {
        Collaborator collaborator =
                new Collaborator();

        collaborator.setName("Membership Tester");
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
            Long managerId
    ) {
        Project project = new Project();

        project.setName("Membership Test Project");
        project.setStatus("PLANNED");
        project.setManagerId(managerId);

        return projectRepository.saveAndFlush(project);
    }

    private ProjectMembership createMembership(
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
