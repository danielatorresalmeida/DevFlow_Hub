package com.devflowhub.backend.security;

import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.exception.AuthenticationFailedException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentCollaboratorResolverTest {

    @Mock
    private CollaboratorRepository collaboratorRepository;

    private CurrentCollaboratorResolver resolver;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        resolver = new CurrentCollaboratorResolver(
                collaboratorRepository
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getRequiredResolvesActiveCollaboratorFromJwtSubject() {
        Collaborator collaborator = collaborator(42L, true);
        authenticateAs("42");

        when(collaboratorRepository.findById(42L))
                .thenReturn(Optional.of(collaborator));

        Collaborator result = resolver.getRequired();

        assertThat(result).isSameAs(collaborator);
        verify(collaboratorRepository).findById(42L);
    }

    @Test
    void getRequiredIdReturnsResolvedCollaboratorId() {
        Collaborator collaborator = collaborator(7L, true);
        authenticateAs("7");

        when(collaboratorRepository.findById(7L))
                .thenReturn(Optional.of(collaborator));

        assertThat(resolver.getRequiredId()).isEqualTo(7L);
    }

    @Test
    void getRequiredRejectsMissingAuthentication() {
        assertThatThrownBy(resolver::getRequired)
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Authentication is required.");

        verify(collaboratorRepository, never()).findById(1L);
    }

    @Test
    void getRequiredRejectsAuthenticatedNonJwtPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user",
                        "credentials",
                        List.of()
                )
        );

        assertThatThrownBy(resolver::getRequired)
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Authentication is required.");
    }

    @Test
    void getRequiredRejectsInvalidJwtSubject() {
        authenticateAs("not-a-collaborator-id");

        assertThatThrownBy(resolver::getRequired)
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Authentication is invalid.");
    }

    @Test
    void getRequiredRejectsMissingCollaborator() {
        authenticateAs("42");

        when(collaboratorRepository.findById(42L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(resolver::getRequired)
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage(
                        "The authenticated collaborator is not active."
                );
    }

    @Test
    void getRequiredRejectsInactiveCollaborator() {
        authenticateAs("42");

        when(collaboratorRepository.findById(42L))
                .thenReturn(Optional.of(collaborator(42L, false)));

        assertThatThrownBy(resolver::getRequired)
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage(
                        "The authenticated collaborator is not active."
                );
    }

    private void authenticateAs(String subject) {
        Instant issuedAt = Instant.now();

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of())
        );
    }

    private Collaborator collaborator(Long id, boolean active) {
        Collaborator collaborator = new Collaborator();
        collaborator.setId(id);
        collaborator.setName("Authorization Test");
        collaborator.setEmail("authorization@example.com");
        collaborator.setPassword("stored-hash");
        collaborator.setRole("Developer");
        collaborator.setActive(active);
        return collaborator;
    }
}
