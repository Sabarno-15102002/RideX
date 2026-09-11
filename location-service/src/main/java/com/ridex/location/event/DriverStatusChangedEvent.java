package com.ridex.location.event;

import java.time.Instant;
import java.util.UUID;

public record DriverStatusChangedEvent(
        UUID eventId,
        UUID driverId,
        UUID userId,
        String status,
        Instant changedAt
) {}