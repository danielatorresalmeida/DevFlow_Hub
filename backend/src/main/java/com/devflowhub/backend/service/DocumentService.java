package com.devflowhub.backend.service;

import com.devflowhub.backend.entity.Attachment;
import com.devflowhub.backend.entity.Document;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.DocumentRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import com.devflowhub.backend.repository.TaskRepository;
import com.devflowhub.backend.util.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final AttachmentService attachmentService;

    public DocumentService(
            DocumentRepository documentRepository,
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            AttachmentService attachmentService
    ) {
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.attachmentService = attachmentService;
    }

    public Optional<Document> findById(Long id) {
        return documentRepository.findById(id);
    }

    public Document getRequired(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Document not found."
                        )
                );
    }

    public List<Document> findByProjectId(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException(
                    "Project not found."
            );
        }

        return documentRepository
                .findByProjectIdOrderByUpdatedAtDesc(projectId);
    }

    public List<Document> findByTaskId(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException(
                    "Task not found."
            );
        }

        return documentRepository
                .findByTaskIdOrderByUpdatedAtDesc(taskId);
    }

    @Transactional
    public Document create(Document document) {
        document.setId(null);
        document.setCreatedAt(null);
        document.setUpdatedAt(null);

        prepareAndValidate(document);

        return documentRepository.save(document);
    }

    @Transactional
    public Document update(
            Long id,
            Document updatedData
    ) {
        Document existing = getRequired(id);

        prepareAndValidate(updatedData);

        existing.setTitle(updatedData.getTitle());
        existing.setContent(updatedData.getContent());
        existing.setProjectId(updatedData.getProjectId());
        existing.setTaskId(updatedData.getTaskId());

        return documentRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Document document = getRequired(id);

        for (
                Attachment attachment
                : attachmentService.findByDocumentId(id)
        ) {
            attachmentService.delete(
                    attachment.getId()
            );
        }

        documentRepository.delete(document);
        documentRepository.flush();
    }

    private void prepareAndValidate(Document document) {
        document.setTitle(
                TextNormalizer.trim(document.getTitle())
        );

        document.setContent(
                TextNormalizer.trimToNull(document.getContent())
        );

        boolean hasProject = document.getProjectId() != null;
        boolean hasTask = document.getTaskId() != null;

        if (hasProject == hasTask) {
            throw new InvalidOperationException(
                    "A document must belong to exactly one project or task."
            );
        }

        if (hasProject
                && !projectRepository.existsById(
                        document.getProjectId()
                )) {
            throw new InvalidOperationException(
                    "The selected project does not exist."
            );
        }

        if (hasTask
                && !taskRepository.existsById(
                        document.getTaskId()
                )) {
            throw new InvalidOperationException(
                    "The selected task does not exist."
            );
        }
    }
}