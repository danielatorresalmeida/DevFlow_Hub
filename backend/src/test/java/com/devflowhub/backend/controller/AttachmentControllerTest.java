package com.devflowhub.backend.controller;

import com.devflowhub.backend.dto.AttachmentDownload;
import com.devflowhub.backend.entity.Attachment;
import com.devflowhub.backend.exception.ApiExceptionHandler;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.service.AttachmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AttachmentControllerTest {

    @Mock
    private AttachmentService attachmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new AttachmentController(
                                attachmentService
                        )
                )
                .setControllerAdvice(
                        new ApiExceptionHandler()
                )
                .build();
    }

    @Test
    void findByIdReturnsPublicMetadataWithoutStorageKey()
            throws Exception {
        Attachment attachment = createAttachment(
                3L,
                7L,
                "report.pdf",
                "documents/7/private-storage-key.pdf"
        );

        when(attachmentService.getRequired(3L))
                .thenReturn(attachment);

        mockMvc.perform(get("/api/attachments/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.documentId").value(7))
                .andExpect(jsonPath("$.originalFilename")
                        .value("report.pdf"))
                .andExpect(jsonPath("$.contentType")
                        .value("application/pdf"))
                .andExpect(jsonPath("$.sizeBytes").value(1024))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-07-25T10:30:00"))
                .andExpect(jsonPath("$.storageKey")
                        .doesNotExist());
    }

    @Test
    void findByDocumentIdReturnsPublicMetadataList()
            throws Exception {
        Attachment attachment = createAttachment(
                4L,
                8L,
                "notes.txt",
                "documents/8/private-notes-key.txt"
        );

        attachment.setContentType("text/plain");
        attachment.setSizeBytes(20L);

        when(attachmentService.findByDocumentId(8L))
                .thenReturn(List.of(attachment));

        mockMvc.perform(
                        get("/api/documents/8/attachments")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4))
                .andExpect(jsonPath("$[0].documentId").value(8))
                .andExpect(jsonPath("$[0].originalFilename")
                        .value("notes.txt"))
                .andExpect(jsonPath("$[0].contentType")
                        .value("text/plain"))
                .andExpect(jsonPath("$[0].sizeBytes").value(20))
                .andExpect(jsonPath("$[0].storageKey")
                        .doesNotExist());
    }

    @Test
    void findByIdReturnsHttp404WhenAttachmentDoesNotExist()
            throws Exception {
        when(attachmentService.getRequired(99L))
                .thenThrow(new ResourceNotFoundException(
                        "Attachment not found."
                ));

        mockMvc.perform(get("/api/attachments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Attachment not found."));
    }

    @Test
    void uploadReturnsCreatedPublicMetadata()
            throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "content".getBytes()
        );

        Attachment attachment = createAttachment(
                3L,
                7L,
                "report.pdf",
                "documents/7/private-key"
        );

        when(attachmentService.upload(
                eq(7L),
                any(MultipartFile.class)
        )).thenReturn(attachment);

        mockMvc.perform(
                        multipart(
                                "/api/documents/7/attachments"
                        )
                                .file(file)
                )
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "/api/attachments/3"
                ))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.originalFilename")
                        .value("report.pdf"))
                .andExpect(jsonPath("$.storageKey")
                        .doesNotExist());
    }

    @Test
    void downloadReturnsFileHeadersAndContent()
            throws Exception {
        Attachment attachment = createAttachment(
                3L,
                7L,
                "report.pdf",
                "documents/7/private-key"
        );

        ByteArrayResource resource =
                new ByteArrayResource(
                        "content".getBytes()
                );

        when(attachmentService.download(3L))
                .thenReturn(
                        new AttachmentDownload(
                                attachment,
                                resource
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/attachments/3/content"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_TYPE,
                        "application/pdf"
                ))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment")
                ))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("report.pdf")
                ))
                .andExpect(content().bytes(
                        "content".getBytes()
                ));
    }

    @Test
    void deleteReturnsNoContent()
            throws Exception {
        mockMvc.perform(
                        delete("/api/attachments/3")
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(attachmentService).delete(3L);
    }

    private Attachment createAttachment(
            Long id,
            Long documentId,
            String filename,
            String storageKey
    ) {
        Attachment attachment = new Attachment();
        attachment.setId(id);
        attachment.setDocumentId(documentId);
        attachment.setOriginalFilename(filename);
        attachment.setStorageKey(storageKey);
        attachment.setContentType("application/pdf");
        attachment.setSizeBytes(1024L);
        attachment.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        7,
                        25,
                        10,
                        30
                )
        );

        return attachment;
    }
}