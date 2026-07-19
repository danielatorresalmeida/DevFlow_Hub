package com.devflowhub.backend.service;

import com.devflowhub.backend.domain.DomainValues;
import com.devflowhub.backend.entity.InternalProgram;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.InternalProgramRepository;
import com.devflowhub.backend.util.TextNormalizer;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class InternalProgramService {

    private final InternalProgramRepository internalProgramRepository;
    private final CollaboratorRepository collaboratorRepository;

    public InternalProgramService(
            InternalProgramRepository internalProgramRepository,
            CollaboratorRepository collaboratorRepository
    ) {
        this.internalProgramRepository = internalProgramRepository;
        this.collaboratorRepository = collaboratorRepository;
    }

    public List<InternalProgram> findAll() {
        return internalProgramRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public Optional<InternalProgram> findById(Long id) {
        return internalProgramRepository.findById(id);
    }

    public InternalProgram getRequired(Long id) {
        return internalProgramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Internal program not found."));
    }

    public long count() {
        return internalProgramRepository.count();
    }

    @Transactional
    public InternalProgram create(InternalProgram internalProgram) {
        internalProgram.setId(null);
        prepareAndValidate(internalProgram);
        return internalProgramRepository.save(internalProgram);
    }

    @Transactional
    public InternalProgram update(Long id, InternalProgram updatedData) {
        InternalProgram existing = getRequired(id);
        prepareAndValidate(updatedData);

        existing.setName(updatedData.getName());
        existing.setDescription(updatedData.getDescription());
        existing.setArea(updatedData.getArea());
        existing.setStatus(updatedData.getStatus());
        existing.setStartDate(updatedData.getStartDate());
        existing.setEndDate(updatedData.getEndDate());
        existing.setManagerId(updatedData.getManagerId());

        return internalProgramRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        internalProgramRepository.delete(getRequired(id));
    }

    private void prepareAndValidate(InternalProgram internalProgram) {
        internalProgram.setName(TextNormalizer.trim(internalProgram.getName()));
        internalProgram.setDescription(TextNormalizer.trimToNull(internalProgram.getDescription()));
        internalProgram.setArea(TextNormalizer.trimToNull(internalProgram.getArea()));
        internalProgram.setStatus(TextNormalizer.upperOrDefault(
                internalProgram.getStatus(),
                DomainValues.ProgramStatus.PLANNED
        ));
        DomainValues.requireAllowed(
                internalProgram.getStatus(),
                DomainValues.ProgramStatus.ALL,
                "Program status"
        );

        if (internalProgram.getStartDate() != null
                && internalProgram.getEndDate() != null
                && internalProgram.getEndDate().isBefore(internalProgram.getStartDate())) {
            throw new InvalidOperationException("Program end date cannot be before its start date.");
        }

        if (internalProgram.getManagerId() != null
                && !collaboratorRepository.existsById(internalProgram.getManagerId())) {
            throw new InvalidOperationException("The selected program manager does not exist.");
        }
    }

}
