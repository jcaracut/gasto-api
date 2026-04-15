package com.gasto.application.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UUID userId,
        String email,
        String fullName
) {
    public static AuthResponse of(String token, UUID userId, String email, String fullName) {
        return new AuthResponse(token, "Bearer", userId, email, fullName);
    }
}
