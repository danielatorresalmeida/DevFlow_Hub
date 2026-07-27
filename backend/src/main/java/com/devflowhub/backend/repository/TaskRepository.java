package com.devflowhub.backend.repository;

import com.devflowhub.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    long countByStatus(String status);

    @Query("select coalesce(sum(task.totalTimeSeconds), 0) from Task task")
    Long sumStoredTimeSeconds();
}
