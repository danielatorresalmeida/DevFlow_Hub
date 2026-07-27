package com.devflowhub.backend.service;

import com.devflowhub.backend.entity.Attachment;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.AttachmentRepository;
import com.devflowhub.backend.repository.DocumentRepository;
import com.devflowhub.backend.util.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final DocumentRepository documentRepository;

    public AttachmentService(
            AttachmentRepository attachmentRepository,
            DocumentRepository documentRepository
    ) {
        this.attachmentRepository = attachmentRepository;
        this.documentRepository = documentRepository;
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

    private void prepareAndValidate(Attachment attachment) {
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

        if (attachment.getStorageKey() != null
                && attachmentRepository.existsByStorageKey(
                        attachment.getStorageKey()
                )) {
            throw new InvalidOperationException(
                    "The storage key is already in use."
            );
        }
    }
}