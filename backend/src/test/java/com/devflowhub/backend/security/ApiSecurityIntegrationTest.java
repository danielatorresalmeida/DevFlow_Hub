package com.devflowhub.backend.security;

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
        Collaborator collaborator = new Collaborator();
        collaborator.setName("Security Test");
        collaborator.setEmail(
                "security-" + UUID.randomUUID() + "@example.com"
        );
        collaborator.setPassword(passwordEncoder.encode(rawPassword));
        collaborator.setRole("Developer");
        collaborator.setActive(true);

        return collaboratorRepository.saveAndFlush(collaborator);
    }
}
