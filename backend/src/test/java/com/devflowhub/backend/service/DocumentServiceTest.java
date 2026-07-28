package com.devflowhub.backend.service;

import com.devflowhub.backend.entity.Attachment;
import com.devflowhub.backend.entity.Document;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.repository.DocumentRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import com.devflowhub.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AttachmentService attachmentService;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
                documentRepository,
                projectRepository,
                taskRepository,
                attachmentService
        );
    }

    @Test
    void createProjectDocumentNormalizesTextAndIgnoresServerFields() {
        Document document = new Document();
        document.setId(99L);
        document.setTitle("  Architecture notes  ");
        document.setContent("  Initial content  ");
        document.setProjectId(7L);
        document.setCreatedAt(
                LocalDateTime.of(2000, 1, 1, 0, 0)
        );
        document.setUpdatedAt(
                LocalDateTime.of(2000, 1, 2, 0, 0)
        );

        when(projectRepository.existsById(7L))
                .thenReturn(true);

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Document result = documentService.create(document);

        assertThat(result.getId()).isNull();
        assertThat(result.getTitle())
                .isEqualTo("Architecture notes");
        assertThat(result.getContent())
                .isEqualTo("Initial content");
        assertThat(result.getProjectId()).isEqualTo(7L);
        assertThat(result.getTaskId()).isNull();
        assertThat(result.getCreatedAt()).isNull();
        assertThat(result.getUpdatedAt()).isNull();
    }

    @Test
    void createTaskDocumentAcceptsExistingTaskOwner() {
        Document document = new Document();
        document.setTitle("Task notes");
        document.setContent("   ");
        document.setTaskId(8L);

        when(taskRepository.existsById(8L))
                .thenReturn(true);

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Document result = documentService.create(document);

        assertThat(result.getProjectId()).isNull();
        assertThat(result.getTaskId()).isEqualTo(8L);
        assertThat(result.getContent()).isNull();
    }

    @Test
    void createRejectsDocumentWithoutOwner() {
        Document document = new Document();
        document.setTitle("Ownerless document");

        assertThatThrownBy(() ->
                documentService.create(document)
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "A document must belong to exactly one project or task."
                );
    }

    @Test
    void createRejectsDocumentWithTwoOwners() {
        Document document = new Document();
        document.setTitle("Invalid document");
        document.setProjectId(1L);
        document.setTaskId(2L);

        assertThatThrownBy(() ->
                documentService.create(document)
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "A document must belong to exactly one project or task."
                );
    }

    @Test
    void createRejectsUnknownProject() {
        Document document = new Document();
        document.setTitle("Project document");
        document.setProjectId(99L);

        when(projectRepository.existsById(99L))
                .thenReturn(false);

        assertThatThrownBy(() ->
                documentService.create(document)
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "The selected project does not exist."
                );
    }

    @Test
    void createRejectsUnknownTask() {
        Document document = new Document();
        document.setTitle("Task document");
        document.setTaskId(99L);

        when(taskRepository.existsById(99L))
                .thenReturn(false);

        assertThatThrownBy(() ->
                documentService.create(document)
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "The selected task does not exist."
                );
    }

    @Test
    void updatePreservesIdentityAndAuditFields() {
        Document existing = new Document();
        existing.setId(5L);
        existing.setTitle("Old title");
        existing.setContent("Old content");
        existing.setProjectId(1L);

        LocalDateTime createdAt =
                LocalDateTime.of(2026, 7, 24, 10, 0);

        LocalDateTime updatedAt =
                LocalDateTime.of(2026, 7, 24, 11, 0);

        existing.setCreatedAt(createdAt);
        existing.setUpdatedAt(updatedAt);

        Document supplied = new Document();
        supplied.setId(999L);
        supplied.setTitle("  New title  ");
        supplied.setContent("  New content  ");
        supplied.setProjectId(2L);
        supplied.setCreatedAt(
                LocalDateTime.of(2000, 1, 1, 0, 0)
        );
        supplied.setUpdatedAt(
                LocalDateTime.of(2000, 1, 2, 0, 0)
        );

        when(documentRepository.findById(5L))
                .thenReturn(Optional.of(existing));

        when(projectRepository.existsById(2L))
                .thenReturn(true);

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Document result =
                documentService.update(5L, supplied);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getTitle()).isEqualTo("New title");
        assertThat(result.getContent())
                .isEqualTo("New content");
        assertThat(result.getProjectId()).isEqualTo(2L);
        assertThat(result.getCreatedAt())
                .isEqualTo(createdAt);
        assertThat(result.getUpdatedAt())
                .isEqualTo(updatedAt);

        ArgumentCaptor<Document> captor =
                ArgumentCaptor.forClass(Document.class);

        verify(documentRepository).save(captor.capture());

        assertThat(captor.getValue()).isSameAs(existing);
    }

@Test
    void deleteRemovesAttachmentsBeforeDocument() {
        Document document = new Document();
        document.setId(5L);

        Attachment firstAttachment = new Attachment();
        firstAttachment.setId(11L);
        firstAttachment.setDocumentId(5L);

        Attachment secondAttachment = new Attachment();
        secondAttachment.setId(12L);
        secondAttachment.setDocumentId(5L);

        when(documentRepository.findById(5L))
                .thenReturn(Optional.of(document));

        when(attachmentService.findByDocumentId(5L))
                .thenReturn(
                        List.of(
                                firstAttachment,
                                secondAttachment
                        )
                );

        documentService.delete(5L);

        InOrder deletionOrder = inOrder(
                documentRepository,
                attachmentService
        );

        deletionOrder.verify(documentRepository)
                .findById(5L);

        deletionOrder.verify(attachmentService)
                .findByDocumentId(5L);

        deletionOrder.verify(attachmentService)
                .delete(11L);

        deletionOrder.verify(attachmentService)
                .delete(12L);

        deletionOrder.verify(documentRepository)
                .delete(document);

        deletionOrder.verify(documentRepository)
                .flush();
    }
}