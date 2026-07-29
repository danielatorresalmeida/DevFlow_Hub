package com.devflowhub.backend.controller;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.dto.ProjectMemberResponse;
import com.devflowhub.backend.dto.ProjectOwnershipTransferResponse;
import com.devflowhub.backend.dto.TransferProjectOwnershipRequest;
import com.devflowhub.backend.exception.ApiExceptionHandler;
import com.devflowhub.backend.exception.ProjectAccessDeniedException;
import com.devflowhub.backend.exception.ResourceConflictException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.service.ProjectOwnershipService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectOwnershipControllerTest {

    @Mock
    private ProjectOwnershipService projectOwnershipService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProjectOwnershipController(
                        projectOwnershipService
                ))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void transferReturnsUpdatedOwnershipAndMapsRequest()
            throws Exception {
        TransferProjectOwnershipRequest request =
                new TransferProjectOwnershipRequest(
                        8L,
                        2L,
                        4L
                );
        when(projectOwnershipService.transfer(11L, request))
                .thenReturn(response());

        mockMvc.perform(post(
                        "/api/projects/11/ownership-transfer"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newOwnerCollaboratorId": 8,
                                  "currentOwnerMembershipVersion": 2,
                                  "newOwnerMembershipVersion": 4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(11))
                .andExpect(jsonPath("$.managerId").value(8))
                .andExpect(jsonPath(
                        "$.previousOwner.collaboratorId"
                ).value(7))
                .andExpect(jsonPath(
                        "$.previousOwner.role"
                ).value("MANAGER"))
                .andExpect(jsonPath(
                        "$.newOwner.collaboratorId"
                ).value(8))
                .andExpect(jsonPath(
                        "$.newOwner.role"
                ).value("OWNER"));

        ArgumentCaptor<TransferProjectOwnershipRequest> captor =
                ArgumentCaptor.forClass(
                        TransferProjectOwnershipRequest.class
                );
        verify(projectOwnershipService).transfer(
                org.mockito.ArgumentMatchers.eq(11L),
                captor.capture()
        );
        assertThat(captor.getValue()).isEqualTo(request);
    }

    @Test
    void transferValidatesRequiredFields() throws Exception {
        mockMvc.perform(post(
                        "/api/projects/11/ownership-transfer"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "The submitted data is invalid."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.newOwnerCollaboratorId"
                ).value(
                        "New owner collaborator ID is required."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.currentOwnerMembershipVersion"
                ).value(
                        "Current owner membership version is required."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.newOwnerMembershipVersion"
                ).value(
                        "New owner membership version is required."
                ));
    }

    @Test
    void hiddenProjectReturnsStructuredNotFound() throws Exception {
        TransferProjectOwnershipRequest request = request();
        when(projectOwnershipService.transfer(11L, request))
                .thenThrow(new ResourceNotFoundException(
                        "Project not found."
                ));

        performValidRequest()
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(
                        "Project not found."
                ));
    }

    @Test
    void nonOwnerReturnsStructuredForbidden() throws Exception {
        TransferProjectOwnershipRequest request = request();
        when(projectOwnershipService.transfer(11L, request))
                .thenThrow(new ProjectAccessDeniedException());

        performValidRequest()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(
                        "You do not have permission to perform this operation."
                ));
    }

    @Test
    void staleMembershipReturnsStructuredConflict() throws Exception {
        TransferProjectOwnershipRequest request = request();
        when(projectOwnershipService.transfer(11L, request))
                .thenThrow(new ResourceConflictException(
                        "New owner membership was modified by another request. Refresh and try again."
                ));

        performValidRequest()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "New owner membership was modified by another request. Refresh and try again."
                ));
    }

    private org.springframework.test.web.servlet.ResultActions
            performValidRequest() throws Exception {
        return mockMvc.perform(post(
                        "/api/projects/11/ownership-transfer"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newOwnerCollaboratorId": 8,
                                  "currentOwnerMembershipVersion": 2,
                                  "newOwnerMembershipVersion": 4
                                }
                                """));
    }

    private TransferProjectOwnershipRequest request() {
        return new TransferProjectOwnershipRequest(
                8L,
                2L,
                4L
        );
    }

    private ProjectOwnershipTransferResponse response() {
        LocalDateTime now = LocalDateTime.of(
                2026,
                7,
                29,
                12,
                0
        );
        return new ProjectOwnershipTransferResponse(
                11L,
                8L,
                new ProjectMemberResponse(
                        1L,
                        11L,
                        7L,
                        "Current Owner",
                        "Product Manager",
                        ProjectMembershipRole.MANAGER,
                        ProjectMembershipStatus.ACTIVE,
                        3L,
                        now,
                        now
                ),
                new ProjectMemberResponse(
                        2L,
                        11L,
                        8L,
                        "New Owner",
                        "Developer",
                        ProjectMembershipRole.OWNER,
                        ProjectMembershipStatus.ACTIVE,
                        5L,
                        now,
                        now
                )
        );
    }
}
