package com.devflowhub.backend.entity;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CollaboratorJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void passwordIsNotIncludedWhenCollaboratorIsSerialized() throws Exception {
        Collaborator collaborator = new Collaborator();
        collaborator.setId(1L);
        collaborator.setName("Ana Silva");
        collaborator.setEmail("ana@example.com");
        collaborator.setPassword("{bcrypt}stored-password-hash");
        collaborator.setRole("Developer");
        collaborator.setActive(true);

        String json = objectMapper.writeValueAsString(collaborator);

        assertThat(json)
                .doesNotContain("password")
                .doesNotContain("stored-password-hash");
    }

    @Test
    void passwordIsAcceptedWhenCollaboratorRequestIsDeserialized() throws Exception {
        String json = """
                {
                  "name": "Ana Silva",
                  "email": "ana@example.com",
                  "password": "new-secret",
                  "role": "Developer",
                  "active": true
                }
                """;

        Collaborator collaborator = objectMapper.readValue(json, Collaborator.class);

        assertThat(collaborator.getPassword()).isEqualTo("new-secret");
    }
}
