package com.devflowhub.backend.controller;

import com.devflowhub.backend.domain.SystemRole;
import com.devflowhub.backend.dto.AuthenticatedCollaboratorResponse;
import com.devflowhub.backend.dto.ChangePasswordRequest;
import com.devflowhub.backend.dto.LoginRequest;
import com.devflowhub.backend.dto.LoginResponse;
import com.devflowhub.backend.exception.ApiExceptionHandler;
import com.devflowhub.backend.exception.AuthenticationFailedException;
import com.devflowhub.backend.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    private AuthController authController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authenticationService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new ApiExceptionHandler())
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver()
                )
                .build();
    }

    @Test
    void loginReturnsJwtAndPublicCollaboratorDataWithoutPassword()
            throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse(
                        "signed.jwt.token",
                        "Bearer",
                        900,
                        new AuthenticatedCollaboratorResponse(
                                1L,
                                "Ana Silva",
                                "ana@example.com",
                                "Developer",
                                SystemRole.USER,
                                true
                        )
                ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ana@example.com",
                                  "password": "current-secret"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.collaborator.id").value(1))
                .andExpect(jsonPath("$.collaborator.email")
                        .value("ana@example.com"))
                .andExpect(jsonPath("$.collaborator.systemRole")
                        .value("USER"))
                .andExpect(jsonPath("$.collaborator.active").value(true))
                .andExpect(jsonPath("$.collaborator.password").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void invalidCredentialsReturnHttp401WithGenericMessage() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new AuthenticationFailedException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ana@example.com",
                                  "password": "wrong-secret"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message")
                        .value("Invalid email or password."))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    @Test
    void changePasswordUsesJwtSubjectAndReturnsHttp204() {
        Jwt jwt = jwtWithSubject("7");
        ChangePasswordRequest request = new ChangePasswordRequest(
                "current-secret",
                "new-secret-123"
        );

        doNothing().when(authenticationService)
                .changePassword(7L, request);

        ResponseEntity<Void> response =
                authController.changePassword(jwt, request);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(authenticationService).changePassword(7L, request);
    }

    @Test
    void invalidChangePasswordPayloadReturnsHttp400() throws Exception {
        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "",
                                  "newPassword": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("The submitted data is invalid."))
                .andExpect(jsonPath(
                        "$.validationErrors.currentPassword"
                ).exists())
                .andExpect(jsonPath(
                        "$.validationErrors.newPassword"
                ).exists());
    }

    private Jwt jwtWithSubject(String subject) {
        Instant issuedAt = Instant.now();

        return Jwt.withTokenValue("signed.jwt.token")
                .header("alg", "HS256")
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(900))
                .build();
    }
}
