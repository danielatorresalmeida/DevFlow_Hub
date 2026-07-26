package com.devflowhub.backend.entity;

import com.devflowhub.backend.repository.AttachmentRepository;
import com.devflowhub.backend.repository.DocumentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DocumentAttachmentPersistenceTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistenceCallbacksCreateAndRefreshDocumentAuditFields() {
        Document document = new Document();
        document.setTitle("Document persistence test");
        document.setContent("Initial content");
        document.setProjectId(1001L);

        Document created =
                documentRepository.saveAndFlush(document);

        Long documentId = created.getId();

        entityManager.clear();

        Document persisted = documentRepository
                .findById(documentId)
                .orElseThrow();

        LocalDateTime createdAt = persisted.getCreatedAt();

        assertThat(createdAt).isNotNull();
        assertThat(persisted.getUpdatedAt())
                .isEqualTo(createdAt);

        persisted.setContent("Updated content");
        persisted.setUpdatedAt(
                LocalDateTime.of(2000, 1, 1, 0, 0)
        );

        documentRepository.saveAndFlush(persisted);

        entityManager.clear();

        Document updated = documentRepository
                .findById(documentId)
                .orElseThrow();

        assertThat(updated.getCreatedAt())
                .isEqualTo(createdAt);

        assertThat(updated.getUpdatedAt())
                .isAfter(LocalDateTime.of(2000, 1, 1, 0, 0));

        assertThat(updated.getContent())
                .isEqualTo("Updated content");
    }

    @Test
    void repositoriesFindDocumentsAndAttachmentMetadata() {
        Document projectDocument = new Document();
        projectDocument.setTitle("Project document");
        projectDocument.setContent("Project content");
        projectDocument.setProjectId(2001L);

        Document savedProjectDocument =
                documentRepository.saveAndFlush(projectDocument);

        Document taskDocument = new Document();
        taskDocument.setTitle("Task document");
        taskDocument.setContent("Task content");
        taskDocument.setTaskId(3001L);

        Document savedTaskDocument =
                documentRepository.saveAndFlush(taskDocument);

        Attachment attachment = new Attachment();
        attachment.setDocumentId(savedProjectDocument.getId());
        attachment.setOriginalFilename("requirements.txt");
        attachment.setStorageKey(
                "documents/test/requirements.txt"
        );
        attachment.setContentType("text/plain");
        attachment.setSizeBytes(128L);

        Attachment savedAttachment =
                attachmentRepository.saveAndFlush(attachment);

        entityManager.clear();

        List<Document> projectDocuments =
                documentRepository
                        .findByProjectIdOrderByUpdatedAtDesc(2001L);

        List<Document> taskDocuments =
                documentRepository
                        .findByTaskIdOrderByUpdatedAtDesc(3001L);

        List<Attachment> attachments =
                attachmentRepository
                        .findByDocumentIdOrderByCreatedAtAsc(
                                savedProjectDocument.getId()
                        );

        assertThat(projectDocuments)
                .extracting(Document::getId)
                .containsExactly(savedProjectDocument.getId());

        assertThat(taskDocuments)
                .extracting(Document::getId)
                .containsExactly(savedTaskDocument.getId());

        assertThat(attachments)
                .extracting(Attachment::getId)
                .containsExactly(savedAttachment.getId());

        assertThat(
                attachmentRepository.findByStorageKey(
                        "documents/test/requirements.txt"
                )
        )
                .isPresent()
                .get()
                .extracting(Attachment::getOriginalFilename)
                .isEqualTo("requirements.txt");

        assertThat(
                attachmentRepository.existsByStorageKey(
                        "documents/test/requirements.txt"
                )
        ).isTrue();

        assertThat(
                attachmentRepository.existsByStorageKey(
                        "documents/test/missing.txt"
                )
        ).isFalse();
    }
}