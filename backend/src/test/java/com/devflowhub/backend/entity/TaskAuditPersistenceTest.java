package com.devflowhub.backend.entity;

import com.devflowhub.backend.repository.TaskRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskAuditPersistenceTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistenceCallbacksCreateAndRefreshAuditFields() {
        Task task = new Task();
        task.setTitle("Audit persistence task");
        task.setStatus("PENDING");
        task.setPriority("MEDIUM");

        Task created = taskRepository.saveAndFlush(task);
        Long taskId = created.getId();

        entityManager.clear();

        Task persisted = taskRepository.findById(taskId).orElseThrow();
        LocalDateTime createdAt = persisted.getCreatedAt();

        assertThat(createdAt).isNotNull();
        assertThat(persisted.getUpdatedAt()).isEqualTo(createdAt);

        persisted.setTitle("Updated audit persistence task");
        persisted.setUpdatedAt(LocalDateTime.of(2000, 1, 1, 0, 0));
        taskRepository.saveAndFlush(persisted);

        entityManager.clear();

        Task updated = taskRepository.findById(taskId).orElseThrow();

        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt())
                .isAfter(LocalDateTime.of(2000, 1, 1, 0, 0));
    }
}
