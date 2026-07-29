package com.devflowhub.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Email must be valid.")
        @Size(max = 150, message = "Email must have at most 150 characters.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(max = 128, message = "Password must have at most 128 characters.")
        String password
) {
}
