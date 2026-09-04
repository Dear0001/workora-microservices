package com.workora.identity.dto;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean emailVerified
) {
}
