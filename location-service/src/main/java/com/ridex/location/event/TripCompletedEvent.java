package com.ridex.location.event;

import java.time.Instant;
import java.util.UUID;

public record TripCompletedEvent(
        UUID eventId,
        UUID tripId,
        UUID riderId,
        UUID driverId,
        Instant completedAt
) {}