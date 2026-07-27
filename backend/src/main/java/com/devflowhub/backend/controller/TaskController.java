package com.devflowhub.backend.controller;

import com.devflowhub.backend.entity.Task;
import com.devflowhub.backend.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> findAll() {
        return taskService.findAll();
    }

    @GetMapping("/{id}")
    public Task findById(@PathVariable Long id) {
        return taskService.getRequired(id);
    }

    @PostMapping
    public ResponseEntity<Task> create(@Valid @RequestBody Task task) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.create(task));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> update(@PathVariable Long id, @Valid @RequestBody Task task) {
        return ResponseEntity.ok(taskService.update(id, task));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/start-timer")
    public Task startTimer(@PathVariable Long id) {
        return taskService.startTimer(id);
    }

    @PostMapping("/{id}/pause-timer")
    public Task pauseTimer(@PathVariable Long id) {
        return taskService.pauseTimer(id);
    }

    @PostMapping("/{id}/resume-timer")
    public Task resumeTimer(@PathVariable Long id) {
        return taskService.resumeTimer(id);
    }

    @GetMapping("/{id}/total-time")
    public Long getTotalTime(@PathVariable Long id) {
        return taskService.getTotalTime(id);
    }

    @GetMapping("/{id}/timer")
    public Task getTimer(@PathVariable Long id) {
        return taskService.getTimer(id);
    }

    @PostMapping("/{id}/complete")
    public Task complete(@PathVariable Long id) {
        return taskService.complete(id);
    }
}
