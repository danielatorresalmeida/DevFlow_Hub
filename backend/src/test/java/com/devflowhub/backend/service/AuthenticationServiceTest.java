package com.devflowhub.backend.service;

import com.devflowhub.backend.dto.ChangePasswordRequest;
import com.devflowhub.backend.dto.LoginRequest;
import com.devflowhub.backend.dto.LoginResponse;
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

    @Mock
    private JwtTokenService jwtTokenService;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                collaboratorRepository,
                passwordEncoder,
                jwtTokenService
        );
    }

    @Test
    void loginReturnsTokenAndPublicCollaboratorDataForValidCredentials() {
        Collaborator collaborator = activeCollaborator();

        when(collaboratorRepository.findByEmailIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(collaborator));
        when(passwordEncoder.matches("current-secret", "{bcrypt}stored-hash"))
                .thenReturn(true);
        when(jwtTokenService.issue(collaborator))
                .thenReturn(new JwtTokenService.IssuedToken(
                        "signed.jwt.token",
                        900
                ));

        LoginResponse response = authenticationService.login(
                new LoginRequest("  ana@example.com  ", "current-secret")
        );

        assertThat(response.accessToken()).isEqualTo("signed.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.collaborator().id()).isEqualTo(1L);
        assertThat(response.collaborator().name()).isEqualTo("Ana Silva");
        assertThat(response.collaborator().email()).isEqualTo("ana@example.com");
        assertThat(response.collaborator().role()).isEqualTo("Developer");
        assertThat(response.collaborator().active()).isTrue();
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
    void changePasswordUsesAuthenticatedCollaboratorAndSavesNewHash() {
        Collaborator collaborator = activeCollaborator();

        when(collaboratorRepository.findById(1L))
                .thenReturn(Optional.of(collaborator));
        when(passwordEncoder.matches(
                "current-secret",
                "{bcrypt}stored-hash"
        )).thenReturn(true);
        when(passwordEncoder.matches(
                "new-secret-123",
                "{bcrypt}stored-hash"
        )).thenReturn(false);
        when(passwordEncoder.encode("new-secret-123"))
                .thenReturn("{bcrypt}new-hash");

        authenticationService.changePassword(
                1L,
                new ChangePasswordRequest(
                        "current-secret",
                        "new-secret-123"
                )
        );

        assertThat(collaborator.getPassword()).isEqualTo("{bcrypt}new-hash");
        verify(collaboratorRepository).save(collaborator);
    }

    @Test
    void changePasswordRejectsInvalidCurrentPassword() {
        Collaborator collaborator = activeCollaborator();

        when(collaboratorRepository.findById(1L))
                .thenReturn(Optional.of(collaborator));
        when(passwordEncoder.matches(
                "wrong-secret",
                "{bcrypt}stored-hash"
        )).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.changePassword(
                1L,
                new ChangePasswordRequest(
                        "wrong-secret",
                        "new-secret-123"
                )
        ))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Current password is invalid.");

        verify(passwordEncoder, never()).encode("new-secret-123");
        verify(collaboratorRepository, never()).save(collaborator);
    }

    @Test
    void changePasswordRejectsReuseOfCurrentPassword() {
        Collaborator collaborator = activeCollaborator();

        when(collaboratorRepository.findById(1L))
                .thenReturn(Optional.of(collaborator));
        when(passwordEncoder.matches(
                "current-secret",
                "{bcrypt}stored-hash"
        )).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.changePassword(
                1L,
                new ChangePasswordRequest(
                        "current-secret",
                        "current-secret"
                )
        ))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage(
                        "New password must be different from the current password."
                );

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
