package com.devflowhub.backend.service;

import com.devflowhub.backend.entity.Document;
import com.devflowhub.backend.entity.Task;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.DocumentRepository;
import com.devflowhub.backend.security.CurrentCollaboratorResolver;
import com.devflowhub.backend.security.ProjectAccessService;
import com.devflowhub.backend.security.ProjectPermission;
import com.devflowhub.backend.security.TaskAccessService;
import com.devflowhub.backend.util.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final ProjectAccessService projectAccessService;
    private final TaskAccessService taskAccessService;
    private final CurrentCollaboratorResolver currentCollaboratorResolver;

    public DocumentService(DocumentRepository documentRepository,
                           ProjectAccessService projectAccessService,
                           TaskAccessService taskAccessService,
                           CurrentCollaboratorResolver currentCollaboratorResolver) {
        this.documentRepository = documentRepository;
        this.projectAccessService = projectAccessService;
        this.taskAccessService = taskAccessService;
        this.currentCollaboratorResolver = currentCollaboratorResolver;
    }

    public Optional<Document> findById(Long id) {
        currentCollaboratorResolver.getRequired();
        return documentRepository.findById(id).map(document -> {
            requireStoredPermission(document, ProjectPermission.VIEW_PROJECT);
            return document;
        });
    }

    public Document getRequired(Long id) {
        return findById(id).orElseThrow(DocumentService::hiddenDocument);
    }

    public List<Document> findByProjectId(Long projectId) {
        currentCollaboratorResolver.getRequired();
        projectAccessService.requirePermission(projectId, ProjectPermission.VIEW_PROJECT);
        return checkedResults(documentRepository.findByProjectIdOrderByUpdatedAtDesc(projectId));
    }

    public List<Document> findByTaskId(Long taskId) {
        currentCollaboratorResolver.getRequired();
        taskAccessService.getRequiredForView(taskId);
        return checkedResults(documentRepository.findByTaskIdOrderByUpdatedAtDesc(taskId));
    }

    private List<Document> checkedResults(List<Document> documents) {
        // Fail closed for legacy/corrupt rows even if database constraints are absent.
        documents.forEach(document -> requireStoredPermission(document, ProjectPermission.VIEW_PROJECT));
        return documents;
    }

    @Transactional
    public Document create(Document document) {
        currentCollaboratorResolver.getRequired();
        prepareAndValidate(document);
        requireParentPermission(document, ProjectPermission.CONTRIBUTE_TO_PROJECT);
        document.setId(null);
        document.setCreatedAt(null);
        document.setUpdatedAt(null);
        return documentRepository.save(document);
    }

    @Transactional
    public Document update(Long id, Document updatedData) {
        Document existing = getRequired(id);
        requireStoredPermission(existing, ProjectPermission.MANAGE_PROJECT);
        prepareAndValidate(updatedData);
        requireParentPermission(updatedData, ProjectPermission.CONTRIBUTE_TO_PROJECT);
        // Authorize both sides before mutating the managed entity.
        existing.setTitle(updatedData.getTitle());
        existing.setContent(updatedData.getContent());
        existing.setProjectId(updatedData.getProjectId());
        existing.setTaskId(updatedData.getTaskId());
        return documentRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Document existing = getRequired(id);
        requireStoredPermission(existing, ProjectPermission.MANAGE_PROJECT);
        documentRepository.delete(existing);
    }

    private void requireStoredPermission(Document document, ProjectPermission permission) {
        if (!hasExactlyOneParent(document)) {
            throw hiddenDocument();
        }
        try {
            requireParentPermission(document, permission);
        } catch (ResourceNotFoundException exception) {
            throw hiddenDocument();
        }
    }

    private void requireParentPermission(Document document, ProjectPermission permission) {
        if (document.getProjectId() != null) {
            projectAccessService.requirePermission(document.getProjectId(), permission);
        } else {
            Task task = taskAccessService.getRequiredForView(document.getTaskId());
            // Standalone access is restricted to its assignee by TaskAccessService.
            // Assignment to a project task does not confer document management rights.
            if (task.getProjectId() != null) {
                projectAccessService.requirePermission(task.getProjectId(), permission);
            }
        }
    }

    private static ResourceNotFoundException hiddenDocument() {
        return new ResourceNotFoundException("Document not found.");
    }

    private static boolean hasExactlyOneParent(Document document) {
        return (document.getProjectId() != null) != (document.getTaskId() != null);
    }

    private void prepareAndValidate(Document document) {
        if (!hasExactlyOneParent(document)) {
            throw new InvalidOperationException("A document must belong to exactly one project or task.");
        }
        document.setTitle(TextNormalizer.trim(document.getTitle()));
        document.setContent(TextNormalizer.trimToNull(document.getContent()));
    }
}
