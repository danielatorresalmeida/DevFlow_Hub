package com.devflowhub.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required.")
    @Size(max = 150, message = "Title must have at most 150 characters.")
    @Column(nullable = false, length = 150)
    private String title;

    @Size(max = 3000, message = "Description must have at most 3000 characters.")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Status is required.")
    @Column(nullable = false, length = 50)
    private String status = "PENDING";

    @NotBlank(message = "Priority is required.")
    @Column(nullable = false, length = 50)
    private String priority = "MEDIUM";

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "total_time_seconds", nullable = false)
    private Long totalTimeSeconds = 0L;

    @Column(name = "timer_active", nullable = false)
    private Boolean timerActive = false;

    @Column(name = "timer_started_at")
    private LocalDateTime timerStartedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Task() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (totalTimeSeconds == null) {
            totalTimeSeconds = 0L;
        }

        if (timerActive == null) {
            timerActive = false;
        }

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public void setTotalTimeSeconds(Long totalTimeSeconds) {
        this.totalTimeSeconds = totalTimeSeconds;
    }

    public Boolean getTimerActive() {
        return timerActive;
    }

    public void setTimerActive(Boolean timerActive) {
        this.timerActive = timerActive;
    }

    public LocalDateTime getTimerStartedAt() {
        return timerStartedAt;
    }

    public void setTimerStartedAt(LocalDateTime timerStartedAt) {
        this.timerStartedAt = timerStartedAt;
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
