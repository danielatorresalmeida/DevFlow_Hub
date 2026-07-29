package com.devflowhub.backend.service;

import com.devflowhub.backend.domain.ProjectMembershipRole;
import com.devflowhub.backend.domain.ProjectMembershipStatus;
import com.devflowhub.backend.dto.ProjectOwnershipTransferResponse;
import com.devflowhub.backend.dto.TransferProjectOwnershipRequest;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.entity.Project;
import com.devflowhub.backend.entity.ProjectMembership;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.exception.ProjectAccessDeniedException;
import com.devflowhub.backend.exception.ResourceConflictException;
import com.devflowhub.backend.exception.ResourceNotFoundException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.repository.ProjectMembershipRepository;
import com.devflowhub.backend.repository.ProjectRepository;
import com.devflowhub.backend.security.ProjectAccessService;
import com.devflowhub.backend.security.ProjectPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectOwnershipServiceTest {

    @Mock
    private ProjectMembershipRepository projectMembershipRepository;

    @Mock
    private CollaboratorRepository collaboratorRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    private ProjectOwnershipService projectOwnershipService;

    @BeforeEach
    void setUp() {
        projectOwnershipService = new ProjectOwnershipService(
                projectMembershipRepository,
                collaboratorRepository,
                projectRepository,
                projectAccessService
        );
    }

    @Test
    void ownerTransfersOwnershipTransactionally() {
        ProjectMembership currentOwner = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                2L
        );
        ProjectMembership newOwner = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.CONTRIBUTOR,
                4L
        );
        Collaborator currentOwnerCollaborator = collaborator(
                7L,
                "Current Owner",
                true
        );
        Collaborator newOwnerCollaborator = collaborator(
                8L,
                "New Owner",
                true
        );
        Project project = project(11L, 7L);

        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.TRANSFER_PROJECT_OWNERSHIP
        )).thenReturn(currentOwner);
        when(projectMembershipRepository
                .countByProjectIdAndRoleAndStatus(
                        11L,
                        ProjectMembershipRole.OWNER,
                        ProjectMembershipStatus.ACTIVE
                )).thenReturn(1L);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                )).thenReturn(Optional.of(newOwner));
        when(collaboratorRepository.findById(7L))
                .thenReturn(Optional.of(currentOwnerCollaborator));
        when(collaboratorRepository.findById(8L))
                .thenReturn(Optional.of(newOwnerCollaborator));
        when(projectRepository.findById(11L))
                .thenReturn(Optional.of(project));
        when(projectMembershipRepository.saveAllAndFlush(any()))
                .thenAnswer(invocation -> {
                    currentOwner.setVersion(3L);
                    newOwner.setVersion(5L);
                    return invocation.getArgument(0);
                });
        when(projectRepository.saveAndFlush(project))
                .thenReturn(project);

        ProjectOwnershipTransferResponse response =
                projectOwnershipService.transfer(
                        11L,
                        new TransferProjectOwnershipRequest(
                                8L,
                                2L,
                                4L
                        )
                );

        assertThat(currentOwner.getRole())
                .isEqualTo(ProjectMembershipRole.MANAGER);
        assertThat(newOwner.getRole())
                .isEqualTo(ProjectMembershipRole.OWNER);
        assertThat(project.getManagerId()).isEqualTo(8L);

        assertThat(response.projectId()).isEqualTo(11L);
        assertThat(response.managerId()).isEqualTo(8L);
        assertThat(response.previousOwner().collaboratorId())
                .isEqualTo(7L);
        assertThat(response.previousOwner().role())
                .isEqualTo(ProjectMembershipRole.MANAGER);
        assertThat(response.previousOwner().version())
                .isEqualTo(3L);
        assertThat(response.newOwner().collaboratorId())
                .isEqualTo(8L);
        assertThat(response.newOwner().role())
                .isEqualTo(ProjectMembershipRole.OWNER);
        assertThat(response.newOwner().version())
                .isEqualTo(5L);

        InOrder order = inOrder(
                projectAccessService,
                projectMembershipRepository,
                collaboratorRepository,
                projectRepository
        );
        order.verify(projectAccessService).requirePermission(
                11L,
                ProjectPermission.TRANSFER_PROJECT_OWNERSHIP
        );
        order.verify(projectMembershipRepository)
                .countByProjectIdAndRoleAndStatus(
                        11L,
                        ProjectMembershipRole.OWNER,
                        ProjectMembershipStatus.ACTIVE
                );
        order.verify(projectMembershipRepository)
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                );
        order.verify(collaboratorRepository).findById(7L);
        order.verify(collaboratorRepository).findById(8L);
        order.verify(projectRepository).findById(11L);
        order.verify(projectMembershipRepository)
                .saveAllAndFlush(List.of(currentOwner, newOwner));
        order.verify(projectRepository).saveAndFlush(project);
    }

    @Test
    void hiddenProjectStopsBeforeOwnershipQueries() {
        doThrow(new ResourceNotFoundException(
                "Project not found."
        )).when(projectAccessService).requirePermission(
                11L,
                ProjectPermission.TRANSFER_PROJECT_OWNERSHIP
        );

        assertThatThrownBy(() -> projectOwnershipService.transfer(
                11L,
                request(8L, 0L, 0L)
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found.");

        verifyNoInteractions(
                projectMembershipRepository,
                collaboratorRepository,
                projectRepository
        );
    }

    @Test
    void nonOwnerCannotTransferOwnership() {
        doThrow(new ProjectAccessDeniedException())
                .when(projectAccessService).requirePermission(
                        11L,
                        ProjectPermission.TRANSFER_PROJECT_OWNERSHIP
                );

        assertThatThrownBy(() -> projectOwnershipService.transfer(
                11L,
                request(8L, 0L, 0L)
        )).isInstanceOf(ProjectAccessDeniedException.class);

        verifyNoInteractions(
                projectMembershipRepository,
                collaboratorRepository,
                projectRepository
        );
    }

    @Test
    void ownerCannotTransferOwnershipToSelf() {
        ProjectMembership currentOwner = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                0L
        );
        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.TRANSFER_PROJECT_OWNERSHIP
        )).thenReturn(currentOwner);

        assertThatThrownBy(() -> projectOwnershipService.transfer(
                11L,
                request(7L, 0L, 0L)
        ))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "Ownership cannot be transferred to the current owner."
                );

        verifyNoInteractions(
                projectMembershipRepository,
                collaboratorRepository,
                projectRepository
        );
    }

    @Test
    void inconsistentOwnerCountReturnsConflict() {
        ProjectMembership currentOwner = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                0L
        );
        when(projectAccessService.requirePermission(
                11L,
                ProjectPermission.TRANSFER_PROJECT_OWNERSHIP
        )).thenReturn(currentOwner);
        when(projectMembershipRepository
                .countByProjectIdAndRoleAndStatus(
                        11L,
                        ProjectMembershipRole.OWNER,
                        ProjectMembershipStatus.ACTIVE
                )).thenReturn(2L);

        assertThatThrownBy(() -> projectOwnershipService.transfer(
                11L,
                request(8L, 0L, 0L)
        ))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage(
                        "Project ownership is inconsistent. Refresh and contact an administrator."
                );

        verify(projectMembershipRepository, never())
                .findByProjectIdAndCollaboratorIdAndStatus(
                        any(),
                        any(),
                        any()
                );
        verifyNoInteractions(collaboratorRepository, projectRepository);
    }

    @Test
    void staleCurrentOwnerVersionReturnsConflict() {
        ProjectMembership currentOwner = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                3L
        );
        allowOwnerTransfer(currentOwner);

        assertThatThrownBy(() -> projectOwnershipService.transfer(
                11L,
                request(8L, 2L, 0L)
        ))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage(
                        "Current owner membership was modified by another request. Refresh and try again."
                );

        verify(projectMembershipRepository, never())
                .findByProjectIdAndCollaboratorIdAndStatus(
                        any(),
                        any(),
                        any()
                );
        verifyNoInteractions(collaboratorRepository, projectRepository);
    }

    @Test
    void newOwnerMustBeActiveProjectMember() {
        ProjectMembership currentOwner = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                0L
        );
        allowOwnerTransfer(currentOwner);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectOwnershipService.transfer(
                11L,
                request(8L, 0L, 0L)
        ))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "The new owner must be an active member of the project."
                );

        verifyNoInteractions(collaboratorRepository, projectRepository);
    }

    @Test
    void staleNewOwnerVersionReturnsConflict() {
        ProjectMembership currentOwner = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                0L
        );
        ProjectMembership newOwner = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.MANAGER,
                5L
        );
        allowOwnerTransfer(currentOwner);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                )).thenReturn(Optional.of(newOwner));

        assertThatThrownBy(() -> projectOwnershipService.transfer(
                11L,
                request(8L, 0L, 4L)
        ))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage(
                        "New owner membership was modified by another request. Refresh and try again."
                );

        verifyNoInteractions(collaboratorRepository, projectRepository);
    }

    @Test
    void existingSecondOwnerReturnsConflict() {
        ProjectMembership currentOwner = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                0L
        );
        ProjectMembership anotherOwner = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.OWNER,
                0L
        );
        allowOwnerTransfer(currentOwner);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                )).thenReturn(Optional.of(anotherOwner));

        assertThatThrownBy(() -> projectOwnershipService.transfer(
                11L,
                request(8L, 0L, 0L)
        ))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage(
                        "The selected collaborator already owns this project."
                );

        verifyNoInteractions(collaboratorRepository, projectRepository);
    }

    @Test
    void inactiveCollaboratorCannotBecomeOwner() {
        ProjectMembership currentOwner = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                0L
        );
        ProjectMembership newOwner = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.MANAGER,
                0L
        );
        allowOwnerTransfer(currentOwner);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                )).thenReturn(Optional.of(newOwner));
        when(collaboratorRepository.findById(7L))
                .thenReturn(Optional.of(collaborator(
                        7L,
                        "Current Owner",
                        true
                )));
        when(collaboratorRepository.findById(8L))
                .thenReturn(Optional.of(collaborator(
                        8L,
                        "Inactive Target",
                        false
                )));

        assertThatThrownBy(() -> projectOwnershipService.transfer(
                11L,
                request(8L, 0L, 0L)
        ))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "The new owner collaborator must be active."
                );

        assertThat(currentOwner.getRole())
                .isEqualTo(ProjectMembershipRole.OWNER);
        assertThat(newOwner.getRole())
                .isEqualTo(ProjectMembershipRole.MANAGER);
        verifyNoInteractions(projectRepository);
        verify(projectMembershipRepository, never())
                .saveAllAndFlush(any());
    }

    @Test
    void missingProjectReturnsNotFoundBeforeMutation() {
        ProjectMembership currentOwner = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                0L
        );
        ProjectMembership newOwner = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.VIEWER,
                0L
        );
        allowOwnerTransfer(currentOwner);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                )).thenReturn(Optional.of(newOwner));
        when(collaboratorRepository.findById(7L))
                .thenReturn(Optional.of(collaborator(
                        7L,
                        "Current Owner",
                        true
                )));
        when(collaboratorRepository.findById(8L))
                .thenReturn(Optional.of(collaborator(
                        8L,
                        "New Owner",
                        true
                )));
        when(projectRepository.findById(11L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectOwnershipService.transfer(
                11L,
                request(8L, 0L, 0L)
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found.");

        assertThat(currentOwner.getRole())
                .isEqualTo(ProjectMembershipRole.OWNER);
        assertThat(newOwner.getRole())
                .isEqualTo(ProjectMembershipRole.VIEWER);
        verify(projectMembershipRepository, never())
                .saveAllAndFlush(any());
    }

    @Test
    void optimisticLockFailureStopsProjectManagerUpdate() {
        ProjectMembership currentOwner = membership(
                1L,
                11L,
                7L,
                ProjectMembershipRole.OWNER,
                0L
        );
        ProjectMembership newOwner = membership(
                2L,
                11L,
                8L,
                ProjectMembershipRole.MANAGER,
                0L
        );
        Project project = project(11L, 7L);
        allowOwnerTransfer(currentOwner);
        when(projectMembershipRepository
                .findByProjectIdAndCollaboratorIdAndStatus(
                        11L,
                        8L,
                        ProjectMembershipStatus.ACTIVE
                )).thenReturn(Optional.of(newOwner));
        when(collaboratorRepository.findById(7L))
                .thenReturn(Optional.of(collaborator(
                        7L,
                        "Current Owner",
                        true
                )));
        when(collaboratorRepository.findById(8L))
                .thenReturn(Optional.of(collaborator(
                        8L,
                        "New Owner",
                        true
                )));
        when(projectRepository.findById(11L))
                .thenReturn(Optional.of(project));
        when(projectMembershipRepository.saveAllAndFlush(any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(
                        ProjectMembership.class,
                        1L
                ));

        assertThatThrownBy(() -> projectOwnershipService.transfer(
                11L,
                request(8L, 0L, 0L)
        )).isInstanceOf(
                ObjectOptimisticLockingFailureException.class
        );

        verify(projectRepository, never()).saveAndFlush(any());
    }

    private void allowOwnerTransfer(ProjectMembership currentOwner) {
        when(projectAccessService.requirePermission(
                currentOwner.getProjectId(),
                ProjectPermission.TRANSFER_PROJECT_OWNERSHIP
        )).thenReturn(currentOwner);
        when(projectMembershipRepository
                .countByProjectIdAndRoleAndStatus(
                        currentOwner.getProjectId(),
                        ProjectMembershipRole.OWNER,
                        ProjectMembershipStatus.ACTIVE
                )).thenReturn(1L);
    }

    private TransferProjectOwnershipRequest request(
            Long collaboratorId,
            Long currentOwnerVersion,
            Long newOwnerVersion
    ) {
        return new TransferProjectOwnershipRequest(
                collaboratorId,
                currentOwnerVersion,
                newOwnerVersion
        );
    }

    private ProjectMembership membership(
            Long id,
            Long projectId,
            Long collaboratorId,
            ProjectMembershipRole role,
            Long version
    ) {
        ProjectMembership membership = new ProjectMembership();
        membership.setId(id);
        membership.setProjectId(projectId);
        membership.setCollaboratorId(collaboratorId);
        membership.setRole(role);
        membership.setStatus(ProjectMembershipStatus.ACTIVE);
        membership.setVersion(version);
        return membership;
    }

    private Collaborator collaborator(
            Long id,
            String name,
            boolean active
    ) {
        Collaborator collaborator = new Collaborator();
        collaborator.setId(id);
        collaborator.setName(name);
        collaborator.setEmail(name.replace(" ", ".") + "@example.com");
        collaborator.setPassword("encoded");
        collaborator.setRole("Developer");
        collaborator.setActive(active);
        return collaborator;
    }

    private Project project(Long id, Long managerId) {
        Project project = new Project();
        project.setId(id);
        project.setName("Project");
        project.setStatus("ACTIVE");
        project.setManagerId(managerId);
        return project;
    }
}
