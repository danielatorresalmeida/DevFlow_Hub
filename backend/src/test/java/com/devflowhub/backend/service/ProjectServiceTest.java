package com.devflowhub.backend.service;

import com.devflowhub.backend.entity.Project;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.ProjectRepository;
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
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CollaboratorRepository collaboratorRepository;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, collaboratorRepository);
    }

    @Test
    void createNormalizesStatusAndText() {
        Project project = new Project();
        project.setName("  Website  ");
        project.setDescription("  New platform  ");
        project.setStatus(" in_progress ");
        project.setManagerId(2L);

        when(collaboratorRepository.existsById(2L)).thenReturn(true);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Project result = projectService.create(project);

        assertThat(result.getName()).isEqualTo("Website");
        assertThat(result.getDescription()).isEqualTo("New platform");
        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void createRejectsEndDateBeforeStartDate() {
        Project project = new Project();
        project.setName("Website");
        project.setStatus("PLANNED");
        project.setStartDate(LocalDate.of(2026, 8, 10));
        project.setEndDate(LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> projectService.create(project))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Project end date cannot be before its start date.");
    }

    @Test
    void createRejectsUnknownManager() {
        Project project = new Project();
        project.setName("Website");
        project.setStatus("PLANNED");
        project.setManagerId(99L);

        when(collaboratorRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> projectService.create(project))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("The selected project manager does not exist.");
    }
}
