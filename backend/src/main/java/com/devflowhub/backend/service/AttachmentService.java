package com.devflowhub.backend.service;

import com.devflowhub.backend.dto.AttachmentDownload;
import com.devflowhub.backend.entity.Attachment;
import com.devflowhub.backend.exception.FileStorageException;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.AttachmentRepository;
import com.devflowhub.backend.repository.DocumentRepository;
import com.devflowhub.backend.storage.AttachmentFileStorage;
import com.devflowhub.backend.util.TextNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AttachmentService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    AttachmentService.class
            );

    private final AttachmentRepository attachmentRepository;
    private final DocumentRepository documentRepository;
    private final AttachmentFileStorage fileStorage;

    public AttachmentService(
            AttachmentRepository attachmentRepository,
            DocumentRepository documentRepository,
            AttachmentFileStorage fileStorage
    ) {
        this.attachmentRepository = attachmentRepository;
        this.documentRepository = documentRepository;
        this.fileStorage = fileStorage;
    }

    public Optional<Attachment> findById(Long id) {
        return attachmentRepository.findById(id);
    }

    public Attachment getRequired(Long id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Attachment not found."
                        )
                );
    }

    public List<Attachment> findByDocumentId(Long documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException(
                    "Document not found."
            );
        }

        return attachmentRepository
                .findByDocumentIdOrderByCreatedAtAsc(documentId);
    }

    @Transactional
    public Attachment create(Attachment attachment) {
        attachment.setId(null);
        attachment.setCreatedAt(null);

        prepareAndValidate(attachment);

        return attachmentRepository.save(attachment);
    }

    @Transactional
    public Attachment upload(
            Long documentId,
            MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new InvalidOperationException(
                    "A non-empty file is required."
            );
        }

        Attachment attachment = new Attachment();
        attachment.setDocumentId(documentId);
        attachment.setOriginalFilename(
                normalizeOriginalFilename(
                        file.getOriginalFilename()
                )
        );
        attachment.setStorageKey(
                generateStorageKey(documentId)
        );
        attachment.setContentType(
                normalizeContentType(
                        file.getContentType()
                )
        );
        attachment.setSizeBytes(file.getSize());

        prepareAndValidate(attachment);

        storeUploadedFile(
                attachment.getStorageKey(),
                file
        );

        boolean rollbackCleanupRegistered;

        try {
            rollbackCleanupRegistered =
                    registerUploadRollbackCleanup(
                            attachment.getStorageKey()
                    );
        }
        catch (RuntimeException exception) {
            cleanupStoredFile(
                    attachment.getStorageKey(),
                    exception
            );

            throw exception;
        }

        try {
            return attachmentRepository
                    .saveAndFlush(attachment);
        }
        catch (RuntimeException exception) {
            if (!rollbackCleanupRegistered) {
                cleanupStoredFile(
                        attachment.getStorageKey(),
                        exception
                );
            }

            throw exception;
        }
    }

    public AttachmentDownload download(Long id) {
        Attachment attachment = getRequired(id);

        return new AttachmentDownload(
                attachment,
                new FileSystemResource(
                        fileStorage.load(
                                attachment.getStorageKey()
                        )
                )
        );
    }

    @Transactional
    public void delete(Long id) {
        Attachment attachment = getRequired(id);

        AttachmentFileStorage.StagedDeletion stagedDeletion =
                fileStorage.stageDelete(
                        attachment.getStorageKey()
                );

        boolean transactionCallbacksRegistered = false;

        try {
            attachmentRepository.delete(attachment);
            attachmentRepository.flush();

            transactionCallbacksRegistered =
                    registerDeleteTransactionCallbacks(
                            stagedDeletion
                    );

            if (!transactionCallbacksRegistered) {
                fileStorage.commitDelete(stagedDeletion);
            }
        }
        catch (RuntimeException exception) {
            if (!transactionCallbacksRegistered) {
                restoreStagedDeletion(
                        stagedDeletion,
                        exception
                );
            }

            throw exception;
        }
    }

    private void storeUploadedFile(
            String storageKey,
            MultipartFile file
    ) {
        try (InputStream inputStream =
                     file.getInputStream()) {
            fileStorage.store(
                    storageKey,
                    inputStream
            );
        }
        catch (IOException exception) {
            throw new FileStorageException(
                    "The uploaded file could not be read.",
                    exception
            );
        }
    }

    private boolean registerUploadRollbackCleanup(
            String storageKey
    ) {
        if (!TransactionSynchronizationManager
                .isSynchronizationActive()) {
            return false;
        }

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCompletion(
                                    int status
                            ) {
                                if (
                                        status
                                                != TransactionSynchronization
                                                .STATUS_COMMITTED
                                ) {
                                    try {
                                        fileStorage.delete(
                                                storageKey
                                        );
                                    }
                                    catch (
                                            RuntimeException
                                                    exception
                                    ) {
                                        LOGGER.error(
                                                "Could not remove attachment file after transaction rollback: {}",
                                                storageKey,
                                                exception
                                        );
                                    }
                                }
                            }
                        }
                );

        return true;
    }

    private boolean registerDeleteTransactionCallbacks(
            AttachmentFileStorage.StagedDeletion stagedDeletion
    ) {
        if (!TransactionSynchronizationManager
                .isSynchronizationActive()) {
            return false;
        }

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCommit() {
                                try {
                                    fileStorage.commitDelete(
                                            stagedDeletion
                                    );
                                }
                                catch (
                                        RuntimeException
                                                exception
                                ) {
                                    LOGGER.error(
                                            "Could not permanently delete staged attachment file: {}",
                                            stagedDeletion.stagedPath(),
                                            exception
                                    );
                                }
                            }

                            @Override
                            public void afterCompletion(
                                    int status
                            ) {
                                if (
                                        status
                                                != TransactionSynchronization
                                                .STATUS_COMMITTED
                                ) {
                                    try {
                                        fileStorage.restoreDelete(
                                                stagedDeletion
                                        );
                                    }
                                    catch (
                                            RuntimeException
                                                    exception
                                    ) {
                                        LOGGER.error(
                                                "Could not restore attachment file after transaction rollback: {}",
                                                stagedDeletion.originalPath(),
                                                exception
                                        );
                                    }
                                }
                            }
                        }
                );

        return true;
    }

    private void cleanupStoredFile(
            String storageKey,
            RuntimeException originalException
    ) {
        try {
            fileStorage.delete(storageKey);
        }
        catch (RuntimeException cleanupException) {
            originalException.addSuppressed(
                    cleanupException
            );
        }
    }

    private void restoreStagedDeletion(
            AttachmentFileStorage.StagedDeletion stagedDeletion,
            RuntimeException originalException
    ) {
        try {
            fileStorage.restoreDelete(
                    stagedDeletion
            );
        }
        catch (RuntimeException restoreException) {
            originalException.addSuppressed(
                    restoreException
            );
        }
    }

    private String generateStorageKey(
            Long documentId
    ) {
        if (documentId == null) {
            throw new InvalidOperationException(
                    "An attachment must belong to a document."
            );
        }

        return "documents/"
                + documentId
                + "/"
                + UUID.randomUUID();
    }

    private String normalizeOriginalFilename(
            String originalFilename
    ) {
        String normalized =
                TextNormalizer.trim(
                        originalFilename
                );

        if (normalized == null || normalized.isBlank()) {
            throw new InvalidOperationException(
                    "Original filename is required."
            );
        }

        normalized = normalized.replace('\\', '/');

        int finalSeparator =
                normalized.lastIndexOf('/');

        if (finalSeparator >= 0) {
            normalized = normalized.substring(
                    finalSeparator + 1
            );
        }

        normalized = TextNormalizer.trim(normalized);

        if (
                normalized == null
                        || normalized.isBlank()
                        || normalized.equals(".")
                        || normalized.equals("..")
                        || normalized
                        .chars()
                        .anyMatch(Character::isISOControl)
        ) {
            throw new InvalidOperationException(
                    "The original filename is invalid."
            );
        }

        if (normalized.length() > 255) {
            throw new InvalidOperationException(
                    "Original filename must have at most 255 characters."
            );
        }

        return normalized;
    }

    private String normalizeContentType(
            String contentType
    ) {
        String normalized =
                TextNormalizer.trimToNull(
                        contentType
                );

        if (
                normalized != null
                        && normalized.length() > 150
        ) {
            throw new InvalidOperationException(
                    "Content type must have at most 150 characters."
            );
        }

        return normalized;
    }

    private void prepareAndValidate(
            Attachment attachment
    ) {
        attachment.setOriginalFilename(
                TextNormalizer.trim(
                        attachment.getOriginalFilename()
                )
        );

        attachment.setStorageKey(
                TextNormalizer.trim(
                        attachment.getStorageKey()
                )
        );

        attachment.setContentType(
                TextNormalizer.trimToNull(
                        attachment.getContentType()
                )
        );

        if (attachment.getDocumentId() == null) {
            throw new InvalidOperationException(
                    "An attachment must belong to a document."
            );
        }

        if (!documentRepository.existsById(
                attachment.getDocumentId()
        )) {
            throw new InvalidOperationException(
                    "The selected document does not exist."
            );
        }

        if (
                attachment.getStorageKey() != null
                        && attachmentRepository
                        .existsByStorageKey(
                                attachment.getStorageKey()
                        )
        ) {
            throw new InvalidOperationException(
                    "The storage key is already in use."
            );
        }
    }
}