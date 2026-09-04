package com.workora.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Token is required")
        @Pattern(regexp = "\\d{6}", message = "OTP must contain exactly 6 digits")
        String token,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must contain at least 8 characters")
        String newPassword
) {
}
