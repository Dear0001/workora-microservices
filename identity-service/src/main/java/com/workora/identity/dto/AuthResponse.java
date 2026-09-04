package com.workora.identity.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String role
) {
}
