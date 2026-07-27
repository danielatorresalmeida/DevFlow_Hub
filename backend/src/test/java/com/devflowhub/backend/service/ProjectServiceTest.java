package com.devflowhub.backend.service;

import com.devflowhub.backend.entity.Document;
import com.devflowhub.backend.entity.Project;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CollaboratorRepository collaboratorRepository;

    @Mock
    private DocumentService documentService;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(
                projectRepository,
                collaboratorRepository,
                documentService
        );
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

@Test
    void deleteRemovesDirectProjectDocumentsBeforeProject() {
        Project project = new Project();
        project.setId(5L);

        Document firstDocument = new Document();
        firstDocument.setId(31L);
        firstDocument.setProjectId(5L);

        Document secondDocument = new Document();
        secondDocument.setId(32L);
        secondDocument.setProjectId(5L);

        when(projectRepository.findById(5L))
                .thenReturn(Optional.of(project));

        when(documentService.findByProjectId(5L))
                .thenReturn(
                        List.of(
                                firstDocument,
                                secondDocument
                        )
                );

        projectService.delete(5L);

        InOrder deletionOrder = inOrder(
                projectRepository,
                documentService
        );

        deletionOrder.verify(projectRepository)
                .findById(5L);

        deletionOrder.verify(documentService)
                .findByProjectId(5L);

        deletionOrder.verify(documentService)
                .delete(31L);

        deletionOrder.verify(documentService)
                .delete(32L);

        deletionOrder.verify(projectRepository)
                .delete(project);

        deletionOrder.verify(projectRepository)
                .flush();
    }
}
