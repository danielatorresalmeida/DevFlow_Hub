package com.devflowhub.backend.repository;

import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("""
            select project
            from Project project
            where project.id in (
                select membership.projectId
                from ProjectMembership membership
                where membership.collaboratorId = :collaboratorId
                  and membership.status = :status
            )
            order by project.name asc, project.id asc
            """)
    List<Project> findAccessibleByCollaboratorIdAndStatus(
            @Param("collaboratorId") Long collaboratorId,
            @Param("status") ProjectMembershipStatus status
    );

    @Query("""
            select count(project)
            from Project project
            where project.id in (
                select membership.projectId
                from ProjectMembership membership
                where membership.collaboratorId = :collaboratorId
                  and membership.status = :status
            )
            """)
    long countAccessibleByCollaboratorIdAndStatus(
            @Param("collaboratorId") Long collaboratorId,
            @Param("status") ProjectMembershipStatus status
    );
}
