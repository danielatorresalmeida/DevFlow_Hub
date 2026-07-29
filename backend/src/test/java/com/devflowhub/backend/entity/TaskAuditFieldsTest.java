package com.devflowhub.backend.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TaskAuditFieldsTest {

    @Test
    void onCreateInitializesBothAuditFields() {
        Task task = new Task();

        task.onCreate();

        assertThat(task.getCreatedAt()).isNotNull();
        assertThat(task.getUpdatedAt()).isEqualTo(task.getCreatedAt());
    }

    @Test
    void onCreateReplacesClientSuppliedAuditValues() {
        Task task = new Task();
        LocalDateTime suppliedCreatedAt = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime suppliedUpdatedAt = LocalDateTime.of(2000, 1, 2, 0, 0);
        task.setCreatedAt(suppliedCreatedAt);
        task.setUpdatedAt(suppliedUpdatedAt);

        task.onCreate();

        assertThat(task.getCreatedAt()).isAfter(suppliedCreatedAt);
        assertThat(task.getUpdatedAt()).isEqualTo(task.getCreatedAt());
    }

    @Test
    void onUpdatePreservesCreatedAtAndRefreshesUpdatedAt() {
        Task task = new Task();
        task.onCreate();

        LocalDateTime createdAt = task.getCreatedAt();
        task.setUpdatedAt(LocalDateTime.of(2000, 1, 1, 0, 0));

        task.onUpdate();

        assertThat(task.getCreatedAt()).isEqualTo(createdAt);
        assertThat(task.getUpdatedAt()).isAfter(LocalDateTime.of(2000, 1, 1, 0, 0));
    }
}
