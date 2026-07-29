package com.devflowhub.backend.controller;

import com.devflowhub.backend.dto.AttachmentMetadataResponse;
import com.devflowhub.backend.service.AttachmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(
            AttachmentService attachmentService
    ) {
        this.attachmentService = attachmentService;
    }

    @GetMapping("/api/attachments/{id}")
    public AttachmentMetadataResponse findById(
            @PathVariable Long id
    ) {
        return AttachmentMetadataResponse.from(
                attachmentService.getRequired(id)
        );
    }

    @GetMapping("/api/documents/{documentId}/attachments")
    public List<AttachmentMetadataResponse> findByDocumentId(
            @PathVariable Long documentId
    ) {
        return attachmentService
                .findByDocumentId(documentId)
                .stream()
                .map(AttachmentMetadataResponse::from)
                .toList();
    }
}