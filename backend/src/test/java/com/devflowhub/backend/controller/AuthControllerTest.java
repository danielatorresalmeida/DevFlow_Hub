package com.devflowhub.backend.controller;

import com.devflowhub.backend.dto.AuthenticatedCollaboratorResponse;
import com.devflowhub.backend.dto.ChangePasswordRequest;
import com.devflowhub.backend.dto.LoginRequest;
import com.devflowhub.backend.exception.ApiExceptionHandler;
import com.devflowhub.backend.exception.AuthenticationFailedException;
import com.devflowhub.backend.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authenticationService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void loginReturnsPublicCollaboratorDataWithoutPassword() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenReturn(new AuthenticatedCollaboratorResponse(
                        1L,
                        "Ana Silva",
                        "ana@example.com",
                        "Developer",
                        true
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
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.active").value(true))
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
                .andExpect(jsonPath("$.message").value("Invalid email or password."))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    @Test
    void changePasswordReturnsHttp204() throws Exception {
        doNothing().when(authenticationService)
                .changePassword(any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ana@example.com",
                                  "currentPassword": "current-secret",
                                  "newPassword": "new-secret-123"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidChangePasswordPayloadReturnsHttp400() throws Exception {
        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "currentPassword": "",
                                  "newPassword": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("The submitted data is invalid."))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.currentPassword").exists())
                .andExpect(jsonPath("$.validationErrors.newPassword").exists());
    }
}
