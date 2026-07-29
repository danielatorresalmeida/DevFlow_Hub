package com.devflowhub.backend.controller;

import com.devflowhub.backend.entity.Document;
import com.devflowhub.backend.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/api/documents/{id}")
    public Document findById(@PathVariable Long id) {
        return documentService.getRequired(id);
    }

    @GetMapping("/api/projects/{projectId}/documents")
    public List<Document> findByProjectId(
            @PathVariable Long projectId
    ) {
        return documentService.findByProjectId(projectId);
    }

    @GetMapping("/api/tasks/{taskId}/documents")
    public List<Document> findByTaskId(
            @PathVariable Long taskId
    ) {
        return documentService.findByTaskId(taskId);
    }

    @PostMapping("/api/documents")
    public ResponseEntity<Document> create(
            @Valid @RequestBody Document document
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(documentService.create(document));
    }

    @PutMapping("/api/documents/{id}")
    public ResponseEntity<Document> update(
            @PathVariable Long id,
            @Valid @RequestBody Document document
    ) {
        return ResponseEntity.ok(
                documentService.update(id, document)
        );
    }

    @DeleteMapping("/api/documents/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}