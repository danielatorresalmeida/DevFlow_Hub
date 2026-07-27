package com.devflowhub.backend.service;

import com.devflowhub.backend.entity.Attachment;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.AttachmentRepository;
import com.devflowhub.backend.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private DocumentRepository documentRepository;

    private AttachmentService attachmentService;

    @BeforeEach
    void setUp() {
        attachmentService = new AttachmentService(
                attachmentRepository,
                documentRepository
        );
    }

    @Test
    void createNormalizesMetadataAndIgnoresServerFields() {
        Attachment attachment = new Attachment();
        attachment.setId(99L);
        attachment.setDocumentId(7L);
        attachment.setOriginalFilename("  report.pdf  ");
        attachment.setStorageKey(
                "  documents/7/report.pdf  "
        );
        attachment.setContentType(
                "  application/pdf  "
        );
        attachment.setSizeBytes(1024L);
        attachment.setCreatedAt(
                LocalDateTime.of(2000, 1, 1, 0, 0)
        );

        when(documentRepository.existsById(7L))
                .thenReturn(true);

        when(attachmentRepository.existsByStorageKey(
                "documents/7/report.pdf"
        )).thenReturn(false);

        when(attachmentRepository.save(any(Attachment.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Attachment result =
                attachmentService.create(attachment);

        assertThat(result.getId()).isNull();
        assertThat(result.getDocumentId()).isEqualTo(7L);
        assertThat(result.getOriginalFilename())
                .isEqualTo("report.pdf");
        assertThat(result.getStorageKey())
                .isEqualTo("documents/7/report.pdf");
        assertThat(result.getContentType())
                .isEqualTo("application/pdf");
        assertThat(result.getSizeBytes()).isEqualTo(1024L);
        assertThat(result.getCreatedAt()).isNull();
    }

    @Test
    void createConvertsBlankContentTypeToNull() {
        Attachment attachment = new Attachment();
        attachment.setDocumentId(8L);
        attachment.setOriginalFilename("notes.txt");
        attachment.setStorageKey(
                "documents/8/notes.txt"
        );
        attachment.setContentType("   ");
        attachment.setSizeBytes(20L);

        when(documentRepository.existsById(8L))
                .thenReturn(true);

        when(attachmentRepository.existsByStorageKey(
                "documents/8/notes.txt"
        )).thenReturn(false);

        when(attachmentRepository.save(any(Attachment.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Attachment result =
                attachmentService.create(attachment);

        assertThat(result.getContentType()).isNull();
    }

    @Test
    void createRejectsMissingDocumentId() {
        Attachment attachment = new Attachment();
        attachment.setOriginalFilename("notes.txt");
        attachment.setStorageKey(
                "documents/notes.txt"
        );
        attachment.setSizeBytes(20L);

        assertThatThrownBy(() ->
                attachmentService.create(attachment)
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "An attachment must belong to a document."
                );
    }

    @Test
    void createRejectsUnknownDocument() {
        Attachment attachment = new Attachment();
        attachment.setDocumentId(99L);
        attachment.setOriginalFilename("notes.txt");
        attachment.setStorageKey(
                "documents/99/notes.txt"
        );
        attachment.setSizeBytes(20L);

        when(documentRepository.existsById(99L))
                .thenReturn(false);

        assertThatThrownBy(() ->
                attachmentService.create(attachment)
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "The selected document does not exist."
                );
    }

    @Test
    void createRejectsDuplicateStorageKey() {
        Attachment attachment = new Attachment();
        attachment.setDocumentId(7L);
        attachment.setOriginalFilename("report.pdf");
        attachment.setStorageKey(
                "documents/7/report.pdf"
        );
        attachment.setSizeBytes(1024L);

        when(documentRepository.existsById(7L))
                .thenReturn(true);

        when(attachmentRepository.existsByStorageKey(
                "documents/7/report.pdf"
        )).thenReturn(true);

        assertThatThrownBy(() ->
                attachmentService.create(attachment)
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "The storage key is already in use."
                );
    }

    @Test
    void findByDocumentIdReturnsAttachmentMetadata() {
        Attachment attachment = new Attachment();
        attachment.setId(2L);
        attachment.setDocumentId(7L);
        attachment.setOriginalFilename("report.pdf");
        attachment.setStorageKey(
                "documents/7/report.pdf"
        );
        attachment.setSizeBytes(1024L);

        when(documentRepository.existsById(7L))
                .thenReturn(true);

        when(attachmentRepository
                .findByDocumentIdOrderByCreatedAtAsc(7L))
                .thenReturn(List.of(attachment));

        List<Attachment> result =
                attachmentService.findByDocumentId(7L);

        assertThat(result)
                .extracting(Attachment::getId)
                .containsExactly(2L);

        verify(attachmentRepository)
                .findByDocumentIdOrderByCreatedAtAsc(7L);
    }

    @Test
    void findByDocumentIdRejectsUnknownDocument() {
        when(documentRepository.existsById(99L))
                .thenReturn(false);

        assertThatThrownBy(() ->
                attachmentService.findByDocumentId(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Document not found.");
    }
}