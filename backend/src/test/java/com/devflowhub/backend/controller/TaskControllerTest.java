package com.devflowhub.backend.controller;

import com.devflowhub.backend.entity.Task;
import com.devflowhub.backend.exception.ApiExceptionHandler;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ProjectAccessDeniedException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TaskController(taskService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createReturnsHttp201() throws Exception {
        Task savedTask = new Task();
        savedTask.setId(1L);
        savedTask.setTitle("Write tests");
        savedTask.setStatus("PENDING");
        savedTask.setPriority("HIGH");
        savedTask.setCreatedAt(LocalDateTime.of(2026, 7, 23, 10, 0));
        savedTask.setUpdatedAt(LocalDateTime.of(2026, 7, 23, 10, 0));

        when(taskService.create(any(Task.class))).thenReturn(savedTask);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Write tests",
                                  "description": "",
                                  "status": "PENDING",
                                  "priority": "HIGH",
                                  "createdAt": "2000-01-01T00:00:00",
                                  "updatedAt": "2000-01-02T00:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Write tests"))
                .andExpect(jsonPath("$.createdAt").value("2026-07-23T10:00:00"))
                .andExpect(jsonPath("$.updatedAt").value("2026-07-23T10:00:00"));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskService).create(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isNull();
        assertThat(captor.getValue().getUpdatedAt()).isNull();
    }

    @Test
    void invalidTimerOperationReturnsHttp400() throws Exception {
        when(taskService.startTimer(1L))
                .thenThrow(new InvalidOperationException("A completed task cannot restart its timer."));

        mockMvc.perform(post("/api/tasks/1/start-timer"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("A completed task cannot restart its timer."));
    }

    @Test
    void hiddenTaskReturnsStructuredHttp404() throws Exception {
        when(taskService.getRequired(1L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Task not found."
                        )
                );

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Task not found."))
                .andExpect(jsonPath("$.validationErrors").isEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());

        verify(taskService).getRequired(1L);
    }

    @Test
    void forbiddenTaskOperationReturnsStructuredHttp403()
            throws Exception {
        when(taskService.complete(1L))
                .thenThrow(
                        new ProjectAccessDeniedException()
                );

        mockMvc.perform(
                        post("/api/tasks/1/complete")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(
                        "You do not have permission " +
                        "to perform this operation."
                ))
                .andExpect(jsonPath("$.validationErrors").isEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());

        verify(taskService).complete(1L);
    }

    @Test
    void malformedJsonReturnsStructuredHttp400WithoutInternalDetails() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Broken JSON",
                                  "status": "PENDING",
                                  "priority":
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("The request body contains invalid JSON."))
                .andExpect(jsonPath("$.validationErrors").isEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }
}
