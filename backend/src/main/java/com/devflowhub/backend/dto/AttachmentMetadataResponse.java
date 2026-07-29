package com.devflowhub.backend.dto;

import com.devflowhub.backend.entity.Attachment;

import java.time.LocalDateTime;

public record AttachmentMetadataResponse(
        Long id,
        Long documentId,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        LocalDateTime createdAt
) {

    public static AttachmentMetadataResponse from(
            Attachment attachment
    ) {
        return new AttachmentMetadataResponse(
                attachment.getId(),
                attachment.getDocumentId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getCreatedAt()
        );
    }
}