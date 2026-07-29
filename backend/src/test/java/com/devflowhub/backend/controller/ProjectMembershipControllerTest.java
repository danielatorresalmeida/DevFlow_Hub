package com.devflowhub.backend.controller;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.dto.AddProjectMemberRequest;
import com.devflowhub.backend.dto.ProjectMemberResponse;
import com.devflowhub.backend.dto.UpdateProjectMemberRequest;
import com.devflowhub.backend.exception.ApiExceptionHandler;
import com.devflowhub.backend.exception.ProjectAccessDeniedException;
import com.devflowhub.backend.exception.ResourceConflictException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.service.ProjectMembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectMembershipControllerTest {

    @Mock
    private ProjectMembershipService projectMembershipService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new ProjectMembershipController(
                                projectMembershipService
                        )
                )
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void findAllReturnsProjectMembers() throws Exception {
        when(projectMembershipService.findAll(11L))
                .thenReturn(List.of(response(
                        ProjectMembershipRole.OWNER,
                        0L
                )));

        mockMvc.perform(get("/api/projects/11/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].projectId").value(11))
                .andExpect(jsonPath("$[0].collaboratorId").value(8))
                .andExpect(jsonPath("$[0].collaboratorName")
                        .value("Ana Silva"))
                .andExpect(jsonPath("$[0].collaboratorRole")
                        .value("Backend Developer"))
                .andExpect(jsonPath("$[0].role")
                        .value("OWNER"))
                .andExpect(jsonPath("$[0].status")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$[0].version").value(0));

        verify(projectMembershipService).findAll(11L);
    }

    @Test
    void addReturnsHttp201AndMapsRequest() throws Exception {
        when(projectMembershipService.add(
                11L,
                new AddProjectMemberRequest(
                        8L,
                        ProjectMembershipRole.CONTRIBUTOR
                )
        )).thenReturn(response(
                ProjectMembershipRole.CONTRIBUTOR,
                0L
        ));

        mockMvc.perform(post("/api/projects/11/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "collaboratorId": 8,
                                  "role": "CONTRIBUTOR"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.collaboratorId").value(8))
                .andExpect(jsonPath("$.role")
                        .value("CONTRIBUTOR"));

        ArgumentCaptor<AddProjectMemberRequest> captor =
                ArgumentCaptor.forClass(
                        AddProjectMemberRequest.class
                );

        verify(projectMembershipService)
                .add(org.mockito.ArgumentMatchers.eq(11L), captor.capture());

        assertThat(captor.getValue().collaboratorId())
                .isEqualTo(8L);
        assertThat(captor.getValue().role())
                .isEqualTo(ProjectMembershipRole.CONTRIBUTOR);
    }

    @Test
    void updateRoleReturnsUpdatedMemberAndMapsVersion()
            throws Exception {
        when(projectMembershipService.updateRole(
                11L,
                8L,
                new UpdateProjectMemberRequest(
                        ProjectMembershipRole.MANAGER,
                        3L
                )
        )).thenReturn(response(
                ProjectMembershipRole.MANAGER,
                4L
        ));

        mockMvc.perform(patch(
                        "/api/projects/11/members/8"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "MANAGER",
                                  "version": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role")
                        .value("MANAGER"))
                .andExpect(jsonPath("$.version").value(4));

        verify(projectMembershipService).updateRole(
                11L,
                8L,
                new UpdateProjectMemberRequest(
                        ProjectMembershipRole.MANAGER,
                        3L
                )
        );
    }

    @Test
    void removeReturnsHttp204() throws Exception {
        mockMvc.perform(delete(
                        "/api/projects/11/members/8"
                ))
                .andExpect(status().isNoContent());

        verify(projectMembershipService).remove(11L, 8L);
    }

    @Test
    void addValidatesRequiredFields() throws Exception {
        mockMvc.perform(post("/api/projects/11/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("The submitted data is invalid."))
                .andExpect(jsonPath(
                        "$.validationErrors.collaboratorId"
                ).value("Collaborator ID is required."))
                .andExpect(jsonPath(
                        "$.validationErrors.role"
                ).value("Membership role is required."));
    }

    @Test
    void updateRoleValidatesRequiredVersion() throws Exception {
        mockMvc.perform(patch(
                        "/api/projects/11/members/8"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "VIEWER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.validationErrors.version"
                ).value("Membership version is required."));
    }

    @Test
    void conflictReturnsStructuredHttp409() throws Exception {
        when(projectMembershipService.add(
                11L,
                new AddProjectMemberRequest(
                        8L,
                        ProjectMembershipRole.CONTRIBUTOR
                )
        )).thenThrow(new ResourceConflictException(
                "The collaborator is already an active member of this project."
        ));

        mockMvc.perform(post("/api/projects/11/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "collaboratorId": 8,
                                  "role": "CONTRIBUTOR"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "The collaborator is already an active member of this project."
                ))
                .andExpect(jsonPath("$.validationErrors")
                        .isEmpty());
    }

    @Test
    void forbiddenMemberOperationReturnsStructuredHttp403()
            throws Exception {
        when(projectMembershipService.updateRole(
                11L,
                8L,
                new UpdateProjectMemberRequest(
                        ProjectMembershipRole.MANAGER,
                        0L
                )
        )).thenThrow(new ProjectAccessDeniedException());

        mockMvc.perform(patch(
                        "/api/projects/11/members/8"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "MANAGER",
                                  "version": 0
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(
                        "You do not have permission to perform this operation."
                ));
    }

    @Test
    void hiddenProjectReturnsStructuredHttp404() throws Exception {
        when(projectMembershipService.findAll(11L))
                .thenThrow(new ResourceNotFoundException(
                        "Project not found."
                ));

        mockMvc.perform(get("/api/projects/11/members"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Project not found."));
    }

    private ProjectMemberResponse response(
            ProjectMembershipRole role,
            Long version
    ) {
        return new ProjectMemberResponse(
                20L,
                11L,
                8L,
                "Ana Silva",
                "Backend Developer",
                role,
                ProjectMembershipStatus.ACTIVE,
                version,
                LocalDateTime.of(2026, 7, 29, 10, 0),
                LocalDateTime.of(2026, 7, 29, 10, 0)
        );
    }
}
