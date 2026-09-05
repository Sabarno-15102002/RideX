package com.ridex.auth.dto.response;

import java.util.UUID;

import com.ridex.auth.utilities.UserRole;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        UserRole role
) {
}