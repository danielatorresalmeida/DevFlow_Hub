package com.devflowhub.backend.service;

import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.exception.InvalidOperationException;
import com.devflowhub.backend.repository.CollaboratorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaboratorServiceTest {

    @Mock
    private CollaboratorRepository collaboratorRepository;

    private CollaboratorService collaboratorService;

    @BeforeEach
    void setUp() {
        collaboratorService = new CollaboratorService(collaboratorRepository);
    }

    @Test
    void createNormalizesValuesBeforeSaving() {
        Collaborator collaborator = collaborator("  Ana Silva  ", "  ANA@EXAMPLE.COM  ", " Developer ", "secret");

        when(collaboratorRepository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(false);
        when(collaboratorRepository.save(any(Collaborator.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Collaborator result = collaboratorService.create(collaborator);

        assertThat(result.getName()).isEqualTo("Ana Silva");
        assertThat(result.getEmail()).isEqualTo("ana@example.com");
        assertThat(result.getRole()).isEqualTo("Developer");
        assertThat(result.getActive()).isTrue();
        verify(collaboratorRepository).save(collaborator);
    }

    @Test
    void createRejectsDuplicateEmail() {
        Collaborator collaborator = collaborator("Ana", "ana@example.com", "Developer", "secret");
        when(collaboratorRepository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> collaboratorService.create(collaborator))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Another collaborator already uses this email address.");
    }

    @Test
    void updateKeepsExistingPasswordWhenNewPasswordIsBlank() {
        Collaborator existing = collaborator("Ana", "ana@example.com", "Developer", "old-password");
        existing.setId(1L);

        Collaborator updatedData = collaborator("Ana Updated", "ana.updated@example.com", "Manager", " ");
        updatedData.setActive(false);

        when(collaboratorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(collaboratorRepository.existsByEmailIgnoreCaseAndIdNot("ana.updated@example.com", 1L))
                .thenReturn(false);
        when(collaboratorRepository.save(existing)).thenReturn(existing);

        Collaborator result = collaboratorService.update(1L, updatedData);

        assertThat(result.getPassword()).isEqualTo("old-password");
        assertThat(result.getName()).isEqualTo("Ana Updated");
        assertThat(result.getActive()).isFalse();
    }

    private Collaborator collaborator(String name, String email, String role, String password) {
        Collaborator collaborator = new Collaborator();
        collaborator.setName(name);
        collaborator.setEmail(email);
        collaborator.setRole(role);
        collaborator.setPassword(password);
        collaborator.setActive(true);
        return collaborator;
    }
}
