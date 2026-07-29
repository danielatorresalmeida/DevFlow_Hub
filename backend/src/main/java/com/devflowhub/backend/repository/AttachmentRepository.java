package com.devflowhub.backend.repository;

import com.devflowhub.backend.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepository
    extends JpaRepository<Attachment, Long> {

    List<Attachment> findByDocumentIdOrderByCreatedAtAsc(Long documentId);

    Optional<Attachment> findByStorageKey(String storageKey);

    boolean existsByStorageKey(String storageKey);
}