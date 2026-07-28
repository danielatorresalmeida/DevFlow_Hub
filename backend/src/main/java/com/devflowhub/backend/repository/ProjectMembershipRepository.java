package com.devflowhub.backend.repository;

import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.entity.ProjectMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMembershipRepository
        extends JpaRepository<ProjectMembership, Long> {

    Optional<ProjectMembership>
            findByProjectIdAndCollaboratorId(
                    Long projectId,
                    Long collaboratorId
            );

    Optional<ProjectMembership>
            findByProjectIdAndCollaboratorIdAndStatus(
                    Long projectId,
                    Long collaboratorId,
                    ProjectMembershipStatus status
            );

    boolean existsByProjectIdAndCollaboratorIdAndStatus(
            Long projectId,
            Long collaboratorId,
            ProjectMembershipStatus status
    );

    List<ProjectMembership>
            findByProjectIdAndStatusOrderByCreatedAtAsc(
                    Long projectId,
                    ProjectMembershipStatus status
            );

    List<ProjectMembership>
            findByCollaboratorIdAndStatusOrderByCreatedAtAsc(
                    Long collaboratorId,
                    ProjectMembershipStatus status
            );
}
