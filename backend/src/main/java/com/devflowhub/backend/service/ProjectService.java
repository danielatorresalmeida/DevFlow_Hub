package com.devflowhub.backend.service;

import com.devflowhub.backend.domain.DomainValues;
import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.entity.Project;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import com.devflowhub.backend.security.CurrentCollaboratorResolver;
import com.devflowhub.backend.security.ProjectAccessService;
import com.devflowhub.backend.security.ProjectPermission;
import com.devflowhub.backend.util.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final CurrentCollaboratorResolver currentCollaboratorResolver;
    private final ProjectAccessService projectAccessService;

    public ProjectService(
            ProjectRepository projectRepository,
            CollaboratorRepository collaboratorRepository,
            CurrentCollaboratorResolver currentCollaboratorResolver,
            ProjectAccessService projectAccessService
    ) {
        this.projectRepository = projectRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.currentCollaboratorResolver = currentCollaboratorResolver;
        this.projectAccessService = projectAccessService;
    }

    public List<Project> findAll() {
        Long collaboratorId = currentCollaboratorResolver.getRequiredId();

        return projectRepository
                .findAccessibleByCollaboratorIdAndStatus(
                        collaboratorId,
                        ProjectMembershipStatus.ACTIVE
                );
    }

    public Optional<Project> findById(Long id) {
        projectAccessService.requirePermission(
                id,
                ProjectPermission.VIEW_PROJECT
        );

        return projectRepository.findById(id);
    }

    public Project getRequired(Long id) {
        projectAccessService.requirePermission(
                id,
                ProjectPermission.VIEW_PROJECT
        );

        return getStoredProjectRequired(id);
    }

    public long count() {
        Long collaboratorId = currentCollaboratorResolver.getRequiredId();

        return projectRepository
                .countAccessibleByCollaboratorIdAndStatus(
                        collaboratorId,
                        ProjectMembershipStatus.ACTIVE
                );
    }

    @Transactional
    public Project create(Project project) {
        project.setId(null);
        prepareAndValidate(project);
        return projectRepository.save(project);
    }

    @Transactional
    public Project update(Long id, Project updatedData) {
        projectAccessService.requirePermission(
                id,
                ProjectPermission.MANAGE_PROJECT
        );

        Project existing = getStoredProjectRequired(id);
        prepareAndValidate(updatedData);

        existing.setName(updatedData.getName());
        existing.setDescription(updatedData.getDescription());
        existing.setStatus(updatedData.getStatus());
        existing.setStartDate(updatedData.getStartDate());
        existing.setEndDate(updatedData.getEndDate());
        existing.setManagerId(updatedData.getManagerId());

        return projectRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        projectAccessService.requirePermission(
                id,
                ProjectPermission.DELETE_PROJECT
        );

        projectRepository.delete(
                getStoredProjectRequired(id)
        );
    }

    private Project getStoredProjectRequired(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found."
                ));
    }

    private void prepareAndValidate(Project project) {
        project.setName(TextNormalizer.trim(project.getName()));
        project.setDescription(TextNormalizer.trimToNull(project.getDescription()));
        project.setStatus(TextNormalizer.upperOrDefault(
                project.getStatus(),
                DomainValues.ProjectStatus.PLANNED
        ));
        DomainValues.requireAllowed(
                project.getStatus(),
                DomainValues.ProjectStatus.ALL,
                "Project status"
        );

        if (project.getStartDate() != null
                && project.getEndDate() != null
                && project.getEndDate().isBefore(project.getStartDate())) {
            throw new InvalidOperationException("Project end date cannot be before its start date.");
        }

        validateCollaborator(project.getManagerId());
    }

    private void validateCollaborator(Long collaboratorId) {
        if (collaboratorId != null && !collaboratorRepository.existsById(collaboratorId)) {
            throw new InvalidOperationException("The selected project manager does not exist.");
        }
    }

}
