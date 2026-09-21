package com.ridex.trip.event.event;

import java.time.Instant;
import java.util.UUID;

public record TripCompletedEvent(
        UUID eventId,
        UUID tripId,
        UUID riderId,
        UUID driverId,
        Instant completedAt
) {}