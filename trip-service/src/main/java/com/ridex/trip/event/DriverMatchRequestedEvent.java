package com.ridex.trip.event;

import java.time.Instant;
import java.util.UUID;

public record DriverMatchRequestedEvent(
        UUID eventId,
        UUID tripId,
        UUID driverId,
        Instant requestedAt
) {}