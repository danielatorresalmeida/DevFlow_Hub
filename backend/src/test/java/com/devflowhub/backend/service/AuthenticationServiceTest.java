package com.devflowhub.backend.service;

import com.devflowhub.backend.dto.AuthenticatedCollaboratorResponse;
import com.devflowhub.backend.dto.ChangePasswordRequest;
import com.devflowhub.backend.dto.LoginRequest;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.exception.AuthenticationFailedException;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private CollaboratorRepository collaboratorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                collaboratorRepository,
                passwordEncoder
        );
    }

    @Test
    void loginReturnsOnlyPublicCollaboratorDataForValidCredentials() {
        Collaborator collaborator = activeCollaborator();

        when(collaboratorRepository.findByEmailIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(collaborator));
        when(passwordEncoder.matches("current-secret", "{bcrypt}stored-hash"))
                .thenReturn(true);

        AuthenticatedCollaboratorResponse response = authenticationService.login(
                new LoginRequest("  ana@example.com  ", "current-secret")
        );

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Ana Silva");
        assertThat(response.email()).isEqualTo("ana@example.com");
        assertThat(response.role()).isEqualTo("Developer");
        assertThat(response.active()).isTrue();
    }

    @Test
    void loginRejectsUnknownCredentialsWithGenericException() {
        when(collaboratorRepository.findByEmailIgnoreCase("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(
                new LoginRequest("unknown@example.com", "wrong-secret")
        ))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void changePasswordEncodesAndSavesNewPassword() {
        Collaborator collaborator = activeCollaborator();

        when(collaboratorRepository.findByEmailIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(collaborator));
        when(passwordEncoder.matches("current-secret", "{bcrypt}stored-hash"))
                .thenReturn(true);
        when(passwordEncoder.matches("new-secret-123", "{bcrypt}stored-hash"))
                .thenReturn(false);
        when(passwordEncoder.encode("new-secret-123"))
                .thenReturn("{bcrypt}new-hash");

        authenticationService.changePassword(
                new ChangePasswordRequest(
                        "ana@example.com",
                        "current-secret",
                        "new-secret-123"
                )
        );

        assertThat(collaborator.getPassword()).isEqualTo("{bcrypt}new-hash");
        verify(collaboratorRepository).save(collaborator);
    }

    @Test
    void changePasswordRejectsReuseOfCurrentPassword() {
        Collaborator collaborator = activeCollaborator();

        when(collaboratorRepository.findByEmailIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(collaborator));
        when(passwordEncoder.matches("current-secret", "{bcrypt}stored-hash"))
                .thenReturn(true);

        assertThatThrownBy(() -> authenticationService.changePassword(
                new ChangePasswordRequest(
                        "ana@example.com",
                        "current-secret",
                        "current-secret"
                )
        ))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("New password must be different from the current password.");

        verify(passwordEncoder, never()).encode("current-secret");
        verify(collaboratorRepository, never()).save(collaborator);
    }

    private Collaborator activeCollaborator() {
        Collaborator collaborator = new Collaborator();
        collaborator.setId(1L);
        collaborator.setName("Ana Silva");
        collaborator.setEmail("ana@example.com");
        collaborator.setPassword("{bcrypt}stored-hash");
        collaborator.setRole("Developer");
        collaborator.setActive(true);
        return collaborator;
    }
}
