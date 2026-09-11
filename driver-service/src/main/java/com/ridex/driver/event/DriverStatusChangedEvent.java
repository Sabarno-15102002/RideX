package com.ridex.driver.event;

import java.time.Instant;
import java.util.UUID;

public record DriverStatusChangedEvent(
        UUID eventId,
        UUID driverId,
        UUID userId,
        String status,
        Instant changedAt
) {}