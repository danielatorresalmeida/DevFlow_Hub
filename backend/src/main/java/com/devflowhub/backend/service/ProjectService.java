package com.devflowhub.backend.service;

import com.devflowhub.backend.domain.DomainValues;
import com.devflowhub.backend.entity.Project;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import com.devflowhub.backend.util.TextNormalizer;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CollaboratorRepository collaboratorRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            CollaboratorRepository collaboratorRepository
    ) {
        this.projectRepository = projectRepository;
        this.collaboratorRepository = collaboratorRepository;
    }

    public List<Project> findAll() {
        return projectRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public Optional<Project> findById(Long id) {
        return projectRepository.findById(id);
    }

    public Project getRequired(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }

    public long count() {
        return projectRepository.count();
    }

    @Transactional
    public Project create(Project project) {
        project.setId(null);
        prepareAndValidate(project);
        return projectRepository.save(project);
    }

    @Transactional
    public Project update(Long id, Project updatedData) {
        Project existing = getRequired(id);
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
        projectRepository.delete(getRequired(id));
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
