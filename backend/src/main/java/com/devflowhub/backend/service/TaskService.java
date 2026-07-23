package com.devflowhub.backend.service;

import com.devflowhub.backend.domain.DomainValues;
import com.devflowhub.backend.entity.Task;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import com.devflowhub.backend.repository.TaskRepository;
import com.devflowhub.backend.util.TextNormalizer;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final Clock clock;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            CollaboratorRepository collaboratorRepository,
            Clock clock
    ) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.clock = clock;
    }

    public List<Task> findAll() {
        return taskRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    public Task getRequired(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found."));
    }

    public long count() {
        return taskRepository.count();
    }

    public long countByStatus(String status) {
        return taskRepository.countByStatus(status);
    }

    public long getStoredTimeSeconds() {
        Long total = taskRepository.sumStoredTimeSeconds();
        return total == null ? 0L : total;
    }

    @Transactional
    public Task create(Task task) {
        task.setId(null);
        task.setTotalTimeSeconds(0L);
        task.setTimerActive(false);
        task.setTimerStartedAt(null);
        task.setCreatedAt(null);
        task.setUpdatedAt(null);
        prepareAndValidate(task);
        return taskRepository.save(task);
    }

    @Transactional
    public Task update(Long id, Task updatedData) {
        Task existing = getRequired(id);
        prepareAndValidate(updatedData);

        existing.setTitle(updatedData.getTitle());
        existing.setDescription(updatedData.getDescription());
        existing.setPriority(updatedData.getPriority());
        existing.setProjectId(updatedData.getProjectId());
        existing.setAssigneeId(updatedData.getAssigneeId());

        // Timer state is changed only through the timer actions.
        if (!Boolean.TRUE.equals(existing.getTimerActive())) {
            existing.setStatus(updatedData.getStatus());
        }

        return taskRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.delete(getRequired(id));
    }

    @Transactional
    public Task complete(Long id) {
        Task task = getRequired(id);

        if (DomainValues.TaskStatus.COMPLETED.equals(task.getStatus())) {
            return task;
        }

        addCurrentSessionToTotal(task);
        task.setTimerActive(false);
        task.setTimerStartedAt(null);
        task.setStatus(DomainValues.TaskStatus.COMPLETED);

        return taskRepository.save(task);
    }

    @Transactional
    public Task startTimer(Long id) {
        Task task = getRequired(id);
        ensureTimerCanStart(task);

        if (!Boolean.TRUE.equals(task.getTimerActive())) {
            task.setTimerActive(true);
            task.setTimerStartedAt(now());
            task.setStatus(DomainValues.TaskStatus.IN_PROGRESS);
        }

        return taskRepository.save(task);
    }

    @Transactional
    public Task pauseTimer(Long id) {
        Task task = getRequired(id);

        if (!Boolean.TRUE.equals(task.getTimerActive()) || task.getTimerStartedAt() == null) {
            throw new InvalidOperationException("The timer is not active.");
        }

        addCurrentSessionToTotal(task);
        task.setTimerActive(false);
        task.setTimerStartedAt(null);

        return taskRepository.save(task);
    }

    @Transactional
    public Task resumeTimer(Long id) {
        return startTimer(id);
    }

    public Long getTotalTime(Long id) {
        Task task = getRequired(id);
        long totalTime = safeTotal(task);

        if (Boolean.TRUE.equals(task.getTimerActive()) && task.getTimerStartedAt() != null) {
            return totalTime + elapsedSeconds(task.getTimerStartedAt());
        }

        return totalTime;
    }

    public Task getTimer(Long id) {
        return getRequired(id);
    }

    private void prepareAndValidate(Task task) {
        task.setTitle(TextNormalizer.trim(task.getTitle()));
        task.setDescription(TextNormalizer.trimToNull(task.getDescription()));
        task.setStatus(TextNormalizer.upperOrDefault(
                task.getStatus(),
                DomainValues.TaskStatus.PENDING
        ));
        task.setPriority(TextNormalizer.upperOrDefault(
                task.getPriority(),
                DomainValues.TaskPriority.MEDIUM
        ));

        DomainValues.requireAllowed(
                task.getStatus(),
                DomainValues.TaskStatus.ALL,
                "Task status"
        );
        DomainValues.requireAllowed(
                task.getPriority(),
                DomainValues.TaskPriority.ALL,
                "Task priority"
        );

        if (task.getProjectId() != null && !projectRepository.existsById(task.getProjectId())) {
            throw new InvalidOperationException("The selected project does not exist.");
        }

        if (task.getAssigneeId() != null && !collaboratorRepository.existsById(task.getAssigneeId())) {
            throw new InvalidOperationException("The selected assignee does not exist.");
        }
    }

    private void ensureTimerCanStart(Task task) {
        if (DomainValues.TaskStatus.COMPLETED.equals(task.getStatus())) {
            throw new InvalidOperationException("A completed task cannot restart its timer.");
        }
    }

    private void addCurrentSessionToTotal(Task task) {
        if (Boolean.TRUE.equals(task.getTimerActive()) && task.getTimerStartedAt() != null) {
            task.setTotalTimeSeconds(safeTotal(task) + elapsedSeconds(task.getTimerStartedAt()));
        }
    }

    private long safeTotal(Task task) {
        return task.getTotalTimeSeconds() == null ? 0L : task.getTotalTimeSeconds();
    }

    private long elapsedSeconds(LocalDateTime start) {
        return Math.max(0L, Duration.between(start, now()).getSeconds());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

}
