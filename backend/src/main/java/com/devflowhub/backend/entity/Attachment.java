package com.devflowhub.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Document ID is required.")
    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @NotBlank(message = "Original filename is required.")
    @Size(
        max = 255,
        message = "Original filename must have at most 255 characters."
    )
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @NotBlank(message = "Storage key is required.")
    @Size(
        max = 500,
        message = "Storage key must have at most 500 characters."
    )
    @Column(
        name = "storage_key",
        nullable = false,
        unique = true,
        length = 500
    )
    private String storageKey;

    @Size(
        max = 150,
        message = "Content type must have at most 150 characters."
    )
    @Column(name = "content_type", length = 150)
    private String contentType;

    @NotNull(message = "File size is required.")
    @PositiveOrZero(message = "File size cannot be negative.")
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Attachment() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}