package com.devflowhub.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Email must be valid.")
        @Size(max = 150, message = "Email must have at most 150 characters.")
        String email,

        @NotBlank(message = "Current password is required.")
        @Size(max = 128, message = "Current password must have at most 128 characters.")
        String currentPassword,

        @NotBlank(message = "New password is required.")
        @Size(
                min = 8,
                max = 64,
                message = "New password must have between 8 and 64 characters."
        )
        String newPassword
) {
}
