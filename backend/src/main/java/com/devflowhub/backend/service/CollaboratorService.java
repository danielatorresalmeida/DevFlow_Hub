package com.devflowhub.backend.service;

import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.util.TextNormalizer;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CollaboratorService {

    private final CollaboratorRepository collaboratorRepository;

    public CollaboratorService(CollaboratorRepository collaboratorRepository) {
        this.collaboratorRepository = collaboratorRepository;
    }

    public List<Collaborator> findAll() {
        return collaboratorRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public Optional<Collaborator> findById(Long id) {
        return collaboratorRepository.findById(id);
    }

    public Collaborator getRequired(Long id) {
        return collaboratorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator not found."));
    }

    public long count() {
        return collaboratorRepository.count();
    }

    @Transactional
    public Collaborator create(Collaborator collaborator) {
        normalize(collaborator);
        validateNewCollaborator(collaborator);
        return collaboratorRepository.save(collaborator);
    }

    @Transactional
    public Collaborator update(Long id, Collaborator updatedData) {
        Collaborator existing = getRequired(id);
        normalize(updatedData);

        if (collaboratorRepository.existsByEmailIgnoreCaseAndIdNot(updatedData.getEmail(), id)) {
            throw new InvalidOperationException("Another collaborator already uses this email address.");
        }

        existing.setName(updatedData.getName());
        existing.setEmail(updatedData.getEmail());
        existing.setRole(updatedData.getRole());
        existing.setActive(Boolean.TRUE.equals(updatedData.getActive()));

        if (updatedData.getPassword() != null && !updatedData.getPassword().isBlank()) {
            existing.setPassword(updatedData.getPassword());
        }

        return collaboratorRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Collaborator collaborator = getRequired(id);
        collaboratorRepository.delete(collaborator);
    }

    // This is a simple academic login. Production systems should use hashed passwords and Spring Security.
    public Optional<Collaborator> authenticate(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }

        return collaboratorRepository.findByEmailIgnoreCase(email.trim())
                .filter(collaborator -> Boolean.TRUE.equals(collaborator.getActive()))
                .filter(collaborator -> password.equals(collaborator.getPassword()));
    }

    private void validateNewCollaborator(Collaborator collaborator) {
        if (collaborator.getPassword() == null || collaborator.getPassword().isBlank()) {
            throw new InvalidOperationException("Password is required when creating a collaborator.");
        }

        if (collaboratorRepository.existsByEmailIgnoreCase(collaborator.getEmail())) {
            throw new InvalidOperationException("Another collaborator already uses this email address.");
        }
    }

    private void normalize(Collaborator collaborator) {
        collaborator.setName(TextNormalizer.trim(collaborator.getName()));
        collaborator.setEmail(TextNormalizer.lower(collaborator.getEmail()));
        collaborator.setRole(TextNormalizer.trim(collaborator.getRole()));

        if (collaborator.getActive() == null) {
            collaborator.setActive(true);
        }
    }

}
