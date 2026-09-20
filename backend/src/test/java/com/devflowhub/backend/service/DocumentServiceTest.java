package com.devflowhub.backend.service;

import com.devflowhub.backend.entity.Document;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.repository.DocumentRepository;
import com.devflowhub.backend.security.ProjectAccessService;
import com.devflowhub.backend.security.TaskAccessService;
import com.devflowhub.backend.security.CurrentCollaboratorResolver;
import com.devflowhub.backend.security.ProjectPermission;
import com.devflowhub.backend.entity.Task;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private TaskAccessService taskAccessService;

    @Mock
    private CurrentCollaboratorResolver currentCollaboratorResolver;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
                documentRepository,
                projectAccessService, taskAccessService, currentCollaboratorResolver
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

        when(taskAccessService.getRequiredForView(8L)).thenReturn(new Task());

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

        when(projectAccessService.requirePermission(99L, ProjectPermission.CONTRIBUTE_TO_PROJECT)).thenThrow(new ResourceNotFoundException("Project not found."));

        assertThatThrownBy(() ->
                documentService.create(document)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Project not found."
                );
    }

    @Test
    void createRejectsUnknownTask() {
        Document document = new Document();
        document.setTitle("Task document");
        document.setTaskId(99L);

        when(taskAccessService.getRequiredForView(99L)).thenThrow(new ResourceNotFoundException("Task not found."));

        assertThatThrownBy(() ->
                documentService.create(document)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Task not found."
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
    void optionalLookupHidesInaccessibleParent() {
        Document existing = new Document();
        existing.setProjectId(1L);
        when(documentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(projectAccessService.requirePermission(1L, ProjectPermission.VIEW_PROJECT))
                .thenThrow(new ResourceNotFoundException("Project not found."));
        assertThatThrownBy(() -> documentService.findById(5L))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Document not found.");
    }

    @Test
    void corruptStoredAssociationCannotBeReadOrListed() {
        Document corrupt = new Document();
        corrupt.setProjectId(1L);
        corrupt.setTaskId(2L);
        when(documentRepository.findById(5L)).thenReturn(Optional.of(corrupt));
        when(documentRepository.findByProjectIdOrderByUpdatedAtDesc(1L))
                .thenReturn(java.util.List.of(corrupt));
        assertThatThrownBy(() -> documentService.getRequired(5L))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Document not found.");
        assertThatThrownBy(() -> documentService.findByProjectId(1L))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Document not found.");
        corrupt.setProjectId(null);
        corrupt.setTaskId(null);
        assertThatThrownBy(() -> documentService.getRequired(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void forbiddenDestinationDoesNotMutateManagedDocument() {
        Document existing = new Document();
        existing.setProjectId(1L);
        existing.setTitle("Original");
        Document target = new Document();
        target.setProjectId(2L);
        target.setTitle("Changed");
        when(documentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(projectAccessService.requirePermission(1L, ProjectPermission.VIEW_PROJECT)).thenReturn(new com.devflowhub.backend.entity.ProjectMembership());
        when(projectAccessService.requirePermission(1L, ProjectPermission.MANAGE_PROJECT)).thenReturn(new com.devflowhub.backend.entity.ProjectMembership());
        when(projectAccessService.requirePermission(2L, ProjectPermission.CONTRIBUTE_TO_PROJECT))
                .thenThrow(new com.devflowhub.backend.exception.ProjectAccessDeniedException());
        assertThatThrownBy(() -> documentService.update(5L, target))
                .isInstanceOf(com.devflowhub.backend.exception.ProjectAccessDeniedException.class);
        assertThat(existing.getTitle()).isEqualTo("Original");
        assertThat(existing.getProjectId()).isEqualTo(1L);
        org.mockito.Mockito.verify(documentRepository, org.mockito.Mockito.never()).save(any());
    }
}
