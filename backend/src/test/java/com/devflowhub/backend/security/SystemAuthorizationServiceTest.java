package com.devflowhub.backend.security;

import com.devflowhub.backend.domain.SystemRole;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.exception.SystemAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemAuthorizationServiceTest {

    @Mock
    private CurrentCollaboratorResolver currentCollaboratorResolver;

    private SystemAuthorizationService systemAuthorizationService;

    @BeforeEach
    void setUp() {
        systemAuthorizationService = new SystemAuthorizationService(
                currentCollaboratorResolver
        );
    }

    @Test
    void requireAdminReturnsActiveAdministrator() {
        Collaborator administrator = collaborator(SystemRole.ADMIN);

        when(currentCollaboratorResolver.getRequired())
                .thenReturn(administrator);

        assertThat(systemAuthorizationService.requireAdmin())
                .isSameAs(administrator);
    }

    @Test
    void requireAdminRejectsStandardUser() {
        when(currentCollaboratorResolver.getRequired())
                .thenReturn(collaborator(SystemRole.USER));

        assertThatThrownBy(systemAuthorizationService::requireAdmin)
                .isInstanceOf(SystemAccessDeniedException.class)
                .hasMessage("Administrator access is required.");
    }

    @Test
    void requireAdminRejectsMissingRole() {
        when(currentCollaboratorResolver.getRequired())
                .thenReturn(collaborator(null));

        assertThatThrownBy(systemAuthorizationService::requireAdmin)
                .isInstanceOf(SystemAccessDeniedException.class)
                .hasMessage("Administrator access is required.");
    }

    private Collaborator collaborator(SystemRole systemRole) {
        Collaborator collaborator = new Collaborator();
        collaborator.setId(1L);
        collaborator.setName("Authorization Test");
        collaborator.setEmail("authorization@example.com");
        collaborator.setPassword("stored-hash");
        collaborator.setRole("Developer");
        collaborator.setSystemRole(systemRole);
        collaborator.setActive(true);
        return collaborator;
    }
}
