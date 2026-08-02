package com.devflowhub.backend.bootstrap;

import com.devflowhub.backend.config.AdminBootstrapProperties;
import com.devflowhub.backend.domain.SystemRole;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.repository.CollaboratorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialAdminBootstrapTest {

    @Mock
    private CollaboratorRepository collaboratorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsDedicatedAdministratorWithNormalizedIdentityAndEncodedPassword() {
        AdminBootstrapProperties properties = properties(
                true,
                "  DevFlow Administrator  ",
                "  ADMIN@EXAMPLE.COM  ",
                "Strong-Admin-2026!"
        );
        InitialAdminBootstrap bootstrap = bootstrap(properties);

        when(collaboratorRepository.existsBySystemRole(SystemRole.ADMIN))
                .thenReturn(false);
        when(collaboratorRepository.existsByEmailIgnoreCase("admin@example.com"))
                .thenReturn(false);
        when(passwordEncoder.encode("Strong-Admin-2026!"))
                .thenReturn("{bcrypt}encoded-password");

        bootstrap.run(null);

        ArgumentCaptor<Collaborator> captor = ArgumentCaptor.forClass(
                Collaborator.class
        );
        verify(collaboratorRepository).save(captor.capture());

        Collaborator administrator = captor.getValue();
        assertThat(administrator.getName()).isEqualTo("DevFlow Administrator");
        assertThat(administrator.getEmail()).isEqualTo("admin@example.com");
        assertThat(administrator.getPassword())
                .isEqualTo("{bcrypt}encoded-password");
        assertThat(administrator.getRole()).isEqualTo("System Administrator");
        assertThat(administrator.getSystemRole()).isEqualTo(SystemRole.ADMIN);
        assertThat(administrator.getActive()).isTrue();
    }

    @Test
    void doesNothingWhenBootstrapIsDisabled() {
        InitialAdminBootstrap bootstrap = bootstrap(properties(
                false,
                "",
                "",
                ""
        ));

        bootstrap.run(null);

        verifyNoInteractions(collaboratorRepository, passwordEncoder);
    }

    @Test
    void refusesToRunWhenAdministratorAlreadyExists() {
        InitialAdminBootstrap bootstrap = bootstrap(properties(
                true,
                "DevFlow Administrator",
                "admin@example.com",
                "Strong-Admin-2026!"
        ));

        when(collaboratorRepository.existsBySystemRole(SystemRole.ADMIN))
                .thenReturn(true);

        assertThatThrownBy(() -> bootstrap.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("administrator already exists");

        verify(collaboratorRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void refusesToReuseExistingEmail() {
        InitialAdminBootstrap bootstrap = bootstrap(properties(
                true,
                "DevFlow Administrator",
                "admin@example.com",
                "Strong-Admin-2026!"
        ));

        when(collaboratorRepository.existsBySystemRole(SystemRole.ADMIN))
                .thenReturn(false);
        when(collaboratorRepository.existsByEmailIgnoreCase("admin@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> bootstrap.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The bootstrap administrator email is already in use.");

        verify(collaboratorRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void rejectsWeakPasswordWithoutPersistingCredentials() {
        InitialAdminBootstrap bootstrap = bootstrap(properties(
                true,
                "DevFlow Administrator",
                "admin@example.com",
                "only-lowercase"
        ));

        when(collaboratorRepository.existsBySystemRole(SystemRole.ADMIN))
                .thenReturn(false);

        assertThatThrownBy(() -> bootstrap.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("uppercase")
                .hasMessageContaining("numeric")
                .hasMessageContaining("special");

        verify(collaboratorRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    private InitialAdminBootstrap bootstrap(
            AdminBootstrapProperties properties
    ) {
        return new InitialAdminBootstrap(
                properties,
                collaboratorRepository,
                passwordEncoder
        );
    }

    private AdminBootstrapProperties properties(
            boolean enabled,
            String name,
            String email,
            String password
    ) {
        return new AdminBootstrapProperties(
                enabled,
                name,
                email,
                password
        );
    }
}
