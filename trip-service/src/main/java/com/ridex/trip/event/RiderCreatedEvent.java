package com.ridex.trip.event;

import java.time.Instant;
import java.util.UUID;

public record RiderCreatedEvent(
        UUID eventId,
        UUID riderId,
        UUID userId,
        Instant createdAt
) {}