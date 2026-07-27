package com.devflowhub.backend.entity;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(
    name = "project_memberships",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "ux_project_memberships_project_collaborator",
            columnNames = {
                "project_id",
                "collaborator_id"
            }
        )
    },
    indexes = {
        @Index(
            name = "idx_project_memberships_project_status",
            columnList = "project_id,status"
        ),
        @Index(
            name = "idx_project_memberships_collaborator_status",
            columnList = "collaborator_id,status"
        )
    }
)
public class ProjectMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Project ID is required.")
    @Column(
        name = "project_id",
        nullable = false,
        updatable = false
    )
    private Long projectId;

    @NotNull(message = "Collaborator ID is required.")
    @Column(
        name = "collaborator_id",
        nullable = false,
        updatable = false
    )
    private Long collaboratorId;

    @NotNull(message = "Membership role is required.")
    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 30
    )
    private ProjectMembershipRole role;

    @NotNull(message = "Membership status is required.")
    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 20
    )
    private ProjectMembershipStatus status =
            ProjectMembershipStatus.ACTIVE;

    @Version
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(nullable = false)
    private Long version = 0L;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(
        name = "updated_at",
        nullable = false
    )
    private LocalDateTime updatedAt;

    public ProjectMembership() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now =
                LocalDateTime.now()
                        .truncatedTo(ChronoUnit.MICROS);

        if (status == null) {
            status = ProjectMembershipStatus.ACTIVE;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt =
                LocalDateTime.now()
                        .truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getCollaboratorId() {
        return collaboratorId;
    }

    public void setCollaboratorId(Long collaboratorId) {
        this.collaboratorId = collaboratorId;
    }

    public ProjectMembershipRole getRole() {
        return role;
    }

    public void setRole(ProjectMembershipRole role) {
        this.role = role;
    }

    public ProjectMembershipStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectMembershipStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
