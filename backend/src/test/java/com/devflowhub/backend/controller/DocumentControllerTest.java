package com.devflowhub.backend.controller;

import com.devflowhub.backend.entity.Document;
import com.devflowhub.backend.exception.ApiExceptionHandler;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.service.DocumentService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private DocumentService documentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new DocumentController(documentService)
                )
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createReturnsHttp201AndIgnoresClientAuditFields()
            throws Exception {
        Document saved = new Document();
        saved.setId(1L);
        saved.setTitle("Architecture notes");
        saved.setContent("Initial content");
        saved.setProjectId(7L);
        saved.setCreatedAt(
                LocalDateTime.of(2026, 7, 25, 10, 0)
        );
        saved.setUpdatedAt(
                LocalDateTime.of(2026, 7, 25, 10, 0)
        );

        when(documentService.create(any(Document.class)))
                .thenReturn(saved);

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Architecture notes",
                                  "content": "Initial content",
                                  "projectId": 7,
                                  "createdAt": "2000-01-01T00:00:00",
                                  "updatedAt": "2000-01-02T00:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title")
                        .value("Architecture notes"))
                .andExpect(jsonPath("$.projectId").value(7))
                .andExpect(jsonPath("$.taskId").isEmpty())
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-07-25T10:00:00"))
                .andExpect(jsonPath("$.updatedAt")
                        .value("2026-07-25T10:00:00"));

        ArgumentCaptor<Document> captor =
                ArgumentCaptor.forClass(Document.class);

        verify(documentService).create(captor.capture());

        assertThat(captor.getValue().getCreatedAt()).isNull();
        assertThat(captor.getValue().getUpdatedAt()).isNull();
    }

    @Test
    void findByProjectReturnsProjectDocuments()
            throws Exception {
        Document document = new Document();
        document.setId(2L);
        document.setTitle("Project requirements");
        document.setProjectId(4L);

        when(documentService.findByProjectId(4L))
                .thenReturn(List.of(document));

        mockMvc.perform(get("/api/projects/4/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].title")
                        .value("Project requirements"))
                .andExpect(jsonPath("$[0].projectId").value(4));
    }

    @Test
    void invalidOwnershipReturnsHttp400()
            throws Exception {
        when(documentService.create(any(Document.class)))
                .thenThrow(new InvalidOperationException(
                        "A document must belong to exactly one project or task."
                ));

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Invalid document"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "A document must belong to exactly one project or task."
                ));
    }

    @Test
    void deleteReturnsHttp204()
            throws Exception {
        mockMvc.perform(delete("/api/documents/5"))
                .andExpect(status().isNoContent());

        verify(documentService).delete(5L);
    }
}