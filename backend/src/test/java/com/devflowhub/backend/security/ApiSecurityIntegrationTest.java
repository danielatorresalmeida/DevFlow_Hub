package com.devflowhub.backend.security;

import com.devflowhub.backend.domain.SystemRole;
import com.devflowhub.backend.entity.Collaborator;
import com.devflowhub.backend.repository.CollaboratorRepository;
import com.devflowhub.backend.service.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void taskEndpointRejectsRequestWithoutBearerToken()
            throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        org.hamcrest.Matchers.startsWith("Bearer")
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required."))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    @Test
    void protectedEndpointRejectsRequestWithoutBearerToken()
            throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        org.hamcrest.Matchers.startsWith("Bearer")
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required."));
    }

    @Test
    void protectedEndpointRejectsInvalidBearerToken() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer invalid-token"
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required."));
    }

    @Test
    void protectedEndpointAcceptsSignedBearerToken() throws Exception {
        Collaborator collaborator = saveActiveCollaborator(
                "current-secret"
        );

        String accessToken = jwtTokenService
                .issue(collaborator)
                .accessToken();

        mockMvc.perform(get("/api/dashboard")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskCount").exists());
    }

    @Test
    void standardUserCannotCreateCollaborator() throws Exception {
        Collaborator collaborator = saveActiveCollaborator(
                "current-secret",
                SystemRole.USER
        );

        String accessToken = jwtTokenService
                .issue(collaborator)
                .accessToken();

        mockMvc.perform(post("/api/collaborators")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Created By Standard User",
                                  "email": "standard-user-create@example.com",
                                  "password": "new-secret-123",
                                  "role": "Developer",
                                  "active": true
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message")
                        .value("Administrator access is required."));
    }

    @Test
    void administratorCanCreateCollaborator() throws Exception {
        Collaborator administrator = saveActiveCollaborator(
                "current-secret",
                SystemRole.ADMIN
        );

        String accessToken = jwtTokenService
                .issue(administrator)
                .accessToken();

        String email = "admin-created-" + UUID.randomUUID()
                + "@example.com";

        mockMvc.perform(post("/api/collaborators")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Created By Administrator",
                                  "email": "%s",
                                  "password": "new-secret-123",
                                  "role": "Developer",
                                  "systemRole": "ADMIN",
                                  "active": true
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value("Created By Administrator"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.systemRole").doesNotExist());

        Collaborator created = collaboratorRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow();

        assertThat(created.getSystemRole()).isEqualTo(SystemRole.USER);
    }

    @Test
    void standardUserCannotCreateInternalProgram() throws Exception {
        Collaborator collaborator = saveActiveCollaborator(
                "current-secret",
                SystemRole.USER
        );

        String accessToken = jwtTokenService
                .issue(collaborator)
                .accessToken();

        mockMvc.perform(post("/api/internal-programs")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Unauthorized Program",
                                  "status": "PLANNED"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message")
                        .value("Administrator access is required."));
    }

    @Test
    void administratorCanCreateInternalProgram() throws Exception {
        Collaborator administrator = saveActiveCollaborator(
                "current-secret",
                SystemRole.ADMIN
        );

        String accessToken = jwtTokenService
                .issue(administrator)
                .accessToken();

        mockMvc.perform(post("/api/internal-programs")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Administrator Program",
                                  "area": "Engineering",
                                  "status": "PLANNED"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value("Administrator Program"))
                .andExpect(jsonPath("$.area").value("Engineering"))
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    void changePasswordUsesCollaboratorIdentityFromJwt() throws Exception {
        Collaborator collaborator = saveActiveCollaborator(
                "current-secret"
        );

        String accessToken = jwtTokenService
                .issue(collaborator)
                .accessToken();

        mockMvc.perform(put("/api/auth/change-password")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "current-secret",
                                  "newPassword": "new-secret-123"
                                }
                                """))
                .andExpect(status().isNoContent());

        Collaborator updated = collaboratorRepository
                .findById(collaborator.getId())
                .orElseThrow();

        assertThat(passwordEncoder.matches(
                "new-secret-123",
                updated.getPassword()
        )).isTrue();
    }

    private Collaborator saveActiveCollaborator(String rawPassword) {
        return saveActiveCollaborator(rawPassword, SystemRole.USER);
    }

    private Collaborator saveActiveCollaborator(
            String rawPassword,
            SystemRole systemRole
    ) {
        Collaborator collaborator = new Collaborator();
        collaborator.setName("Security Test");
        collaborator.setEmail(
                "security-" + UUID.randomUUID() + "@example.com"
        );
        collaborator.setPassword(passwordEncoder.encode(rawPassword));
        collaborator.setRole("Developer");
        collaborator.setSystemRole(systemRole);
        collaborator.setActive(true);

        return collaboratorRepository.saveAndFlush(collaborator);
    }
}
