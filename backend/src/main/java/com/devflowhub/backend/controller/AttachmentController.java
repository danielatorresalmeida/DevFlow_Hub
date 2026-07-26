package com.devflowhub.backend.controller;

import com.devflowhub.backend.dto.AttachmentDownload;
import com.devflowhub.backend.dto.AttachmentMetadataResponse;
import com.devflowhub.backend.entity.Attachment;
import com.devflowhub.backend.service.AttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
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

    @PostMapping(
        value = "/api/documents/{documentId}/attachments",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<AttachmentMetadataResponse> upload(
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file
    ) {
        Attachment attachment =
                attachmentService.upload(
                        documentId,
                        file
                );

        URI location = URI.create(
                "/api/attachments/"
                        + attachment.getId()
        );

        return ResponseEntity
                .created(location)
                .body(
                        AttachmentMetadataResponse.from(
                                attachment
                        )
                );
    }

    @GetMapping("/api/attachments/{id}/content")
    public ResponseEntity<Resource> download(
            @PathVariable Long id
    ) {
        AttachmentDownload download =
                attachmentService.download(id);

        Attachment attachment =
                download.attachment();

        ContentDisposition disposition =
                ContentDisposition
                        .attachment()
                        .filename(
                                attachment
                                        .getOriginalFilename(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity
                .ok()
                .contentType(
                        resolveContentType(
                                attachment.getContentType()
                        )
                )
                .contentLength(
                        attachment.getSizeBytes()
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .body(download.resource());
    }

    @DeleteMapping("/api/attachments/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        attachmentService.delete(id);

        return ResponseEntity
                .noContent()
                .build();
    }

    private MediaType resolveContentType(
            String contentType
    ) {
        if (
                contentType == null
                        || contentType.isBlank()
        ) {
            return MediaType
                    .APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(
                    contentType
            );
        }
        catch (InvalidMediaTypeException exception) {
            return MediaType
                    .APPLICATION_OCTET_STREAM;
        }
    }
}