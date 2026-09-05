package com.ridex.auth.dto.response;

import java.util.UUID;
import com.ridex.auth.utilities.UserRole;

public record RegisterResponse(
        UUID userId,
        String name,
        String email,
        UserRole role
) {
}