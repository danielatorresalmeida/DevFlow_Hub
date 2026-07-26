package com.devflowhub.backend.service;

import com.devflowhub.backend.dto.AttachmentDownload;
import com.devflowhub.backend.entity.Attachment;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.AttachmentRepository;
import com.devflowhub.backend.repository.DocumentRepository;
import com.devflowhub.backend.storage.AttachmentFileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private AttachmentFileStorage fileStorage;

    private AttachmentService attachmentService;

    @BeforeEach
    void setUp() {
        attachmentService = new AttachmentService(
                attachmentRepository,
                documentRepository,
                fileStorage
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
        Attachment attachment = createAttachment();

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

    @Test
    void uploadStoresFileAndMetadata() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "C:\\fakepath\\report.pdf",
                "application/pdf",
                "file content".getBytes()
        );

        when(documentRepository.existsById(7L))
                .thenReturn(true);

        when(attachmentRepository
                .existsByStorageKey(anyString()))
                .thenReturn(false);

        when(attachmentRepository
                .saveAndFlush(any(Attachment.class)))
                .thenAnswer(invocation -> {
                    Attachment attachment =
                            invocation.getArgument(0);

                    attachment.setId(5L);

                    return attachment;
                });

        Attachment result =
                attachmentService.upload(
                        7L,
                        file
                );

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getDocumentId()).isEqualTo(7L);
        assertThat(result.getOriginalFilename())
                .isEqualTo("report.pdf");
        assertThat(result.getContentType())
                .isEqualTo("application/pdf");
        assertThat(result.getSizeBytes())
                .isEqualTo(12L);
        assertThat(result.getStorageKey())
                .startsWith("documents/7/");

        verify(fileStorage).store(
                anyString(),
                any(InputStream.class)
        );

        verify(attachmentRepository)
                .saveAndFlush(result);
    }

    @Test
    void uploadRejectsEmptyFile() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        assertThatThrownBy(() ->
                attachmentService.upload(7L, file)
        )
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "A non-empty file is required."
                );
    }

    @Test
    void uploadDeletesPhysicalFileWhenMetadataSaveFails() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "content".getBytes()
        );

        when(documentRepository.existsById(7L))
                .thenReturn(true);

        when(attachmentRepository
                .existsByStorageKey(anyString()))
                .thenReturn(false);

        when(attachmentRepository
                .saveAndFlush(any(Attachment.class)))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "Database failure."
                        )
                );

        assertThatThrownBy(() ->
                attachmentService.upload(7L, file)
        )
                .isInstanceOf(
                        DataIntegrityViolationException.class
                );

        ArgumentCaptor<String> storageKey =
                ArgumentCaptor.forClass(String.class);

        verify(fileStorage).store(
                storageKey.capture(),
                any(InputStream.class)
        );

        verify(fileStorage).delete(
                storageKey.getValue()
        );
    }

    @Test
    void downloadReturnsPhysicalResource() {
        Attachment attachment = createAttachment();

        Path storedPath =
                Path.of("uploads", "report.pdf");

        when(attachmentRepository.findById(2L))
                .thenReturn(Optional.of(attachment));

        when(fileStorage.load(
                attachment.getStorageKey()
        )).thenReturn(storedPath);

        AttachmentDownload result =
                attachmentService.download(2L);

        assertThat(result.attachment())
                .isSameAs(attachment);

        assertThat(result.resource())
                .isInstanceOf(
                        FileSystemResource.class
                );
    }

    @Test
    void deleteStagesDatabaseAndPhysicalDeletion() {
        Attachment attachment = createAttachment();

        AttachmentFileStorage.StagedDeletion staged =
                new AttachmentFileStorage.StagedDeletion(
                        Path.of("original"),
                        Path.of("staged")
                );

        when(attachmentRepository.findById(2L))
                .thenReturn(Optional.of(attachment));

        when(fileStorage.stageDelete(
                attachment.getStorageKey()
        )).thenReturn(staged);

        attachmentService.delete(2L);

        verify(attachmentRepository)
                .delete(attachment);

        verify(attachmentRepository)
                .flush();

        verify(fileStorage)
                .commitDelete(staged);
    }

    @Test
    void deleteRestoresFileWhenDatabaseDeletionFails() {
        Attachment attachment = createAttachment();

        AttachmentFileStorage.StagedDeletion staged =
                new AttachmentFileStorage.StagedDeletion(
                        Path.of("original"),
                        Path.of("staged")
                );

        when(attachmentRepository.findById(2L))
                .thenReturn(Optional.of(attachment));

        when(fileStorage.stageDelete(
                attachment.getStorageKey()
        )).thenReturn(staged);

        doThrow(
                new DataIntegrityViolationException(
                        "Database failure."
                )
        )
                .when(attachmentRepository)
                .flush();

        assertThatThrownBy(() ->
                attachmentService.delete(2L)
        )
                .isInstanceOf(
                        DataIntegrityViolationException.class
                );

        verify(fileStorage)
                .restoreDelete(staged);
    }

    private Attachment createAttachment() {
        Attachment attachment = new Attachment();
        attachment.setId(2L);
        attachment.setDocumentId(7L);
        attachment.setOriginalFilename("report.pdf");
        attachment.setStorageKey(
                "documents/7/report.pdf"
        );
        attachment.setContentType("application/pdf");
        attachment.setSizeBytes(1024L);

        return attachment;
    }
}