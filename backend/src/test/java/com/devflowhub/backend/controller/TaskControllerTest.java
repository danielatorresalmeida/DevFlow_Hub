package com.devflowhub.backend.controller;

import com.devflowhub.backend.entity.Task;
import com.devflowhub.backend.exception.ApiExceptionHandler;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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

        when(taskService.create(any(Task.class))).thenReturn(savedTask);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Write tests",
                                  "description": "",
                                  "status": "PENDING",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Write tests"));
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
