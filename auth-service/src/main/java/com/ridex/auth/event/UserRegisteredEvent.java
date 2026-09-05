package com.ridex.auth.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID eventId,
        UUID userId,
        String name,
        String role,
        Instant registeredAt
) {
}