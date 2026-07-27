package com.devflowhub.backend.service;

import com.devflowhub.backend.entity.InternalProgram;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.InternalProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalProgramServiceTest {

    @Mock
    private InternalProgramRepository internalProgramRepository;

    @Mock
    private CollaboratorRepository collaboratorRepository;

    private InternalProgramService internalProgramService;

    @BeforeEach
    void setUp() {
        internalProgramService = new InternalProgramService(
                internalProgramRepository,
                collaboratorRepository
        );
    }

    @Test
    void createNormalizesProgramValues() {
        InternalProgram program = new InternalProgram();
        program.setName("  Java Academy  ");
        program.setArea("  Engineering  ");
        program.setStatus(" active ");

        when(internalProgramRepository.save(any(InternalProgram.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InternalProgram result = internalProgramService.create(program);

        assertThat(result.getName()).isEqualTo("Java Academy");
        assertThat(result.getArea()).isEqualTo("Engineering");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void createRejectsInvalidDateRange() {
        InternalProgram program = new InternalProgram();
        program.setName("Java Academy");
        program.setStatus("PLANNED");
        program.setStartDate(LocalDate.of(2026, 9, 20));
        program.setEndDate(LocalDate.of(2026, 9, 10));

        assertThatThrownBy(() -> internalProgramService.create(program))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Program end date cannot be before its start date.");
    }
}
