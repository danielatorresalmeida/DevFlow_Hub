package com.devflowhub.backend.repository;

import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
            select task
            from Task task
            where (
                task.projectId is null
                and task.assigneeId = :collaboratorId
            )
            or task.projectId in (
                select membership.projectId
                from ProjectMembership membership
                where membership.collaboratorId = :collaboratorId
                  and membership.status = :status
            )
            order by task.createdAt desc, task.id desc
            """)
    List<Task> findAccessibleByCollaboratorIdAndStatus(
            @Param("collaboratorId") Long collaboratorId,
            @Param("status") ProjectMembershipStatus status
    );

    @Query("""
            select count(task)
            from Task task
            where (
                task.projectId is null
                and task.assigneeId = :collaboratorId
            )
            or task.projectId in (
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

    @Query("""
            select count(task)
            from Task task
            where task.status = :taskStatus
              and (
                  (
                      task.projectId is null
                      and task.assigneeId = :collaboratorId
                  )
                  or task.projectId in (
                      select membership.projectId
                      from ProjectMembership membership
                      where membership.collaboratorId = :collaboratorId
                        and membership.status = :status
                  )
              )
            """)
    long countAccessibleByCollaboratorIdAndStatusAndTaskStatus(
            @Param("collaboratorId") Long collaboratorId,
            @Param("status") ProjectMembershipStatus status,
            @Param("taskStatus") String taskStatus
    );

    @Query("""
            select coalesce(sum(task.totalTimeSeconds), 0)
            from Task task
            where (
                task.projectId is null
                and task.assigneeId = :collaboratorId
            )
            or task.projectId in (
                select membership.projectId
                from ProjectMembership membership
                where membership.collaboratorId = :collaboratorId
                  and membership.status = :status
            )
            """)
    Long sumAccessibleStoredTimeSecondsByCollaboratorIdAndStatus(
            @Param("collaboratorId") Long collaboratorId,
            @Param("status") ProjectMembershipStatus status
    );
}
