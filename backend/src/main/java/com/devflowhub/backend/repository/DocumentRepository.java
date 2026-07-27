package com.devflowhub.backend.repository;

import com.devflowhub.backend.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByProjectIdOrderByUpdatedAtDesc(Long projectId);

    List<Document> findByTaskIdOrderByUpdatedAtDesc(Long taskId);
}