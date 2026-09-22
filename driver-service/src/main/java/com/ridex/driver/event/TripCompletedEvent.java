package com.ridex.driver.event;

import java.time.Instant;
import java.util.UUID;

public record TripCompletedEvent(
        UUID eventId,
        UUID tripId,
        UUID riderId,
        UUID driverId,

        double pickupLatitude,
        double pickupLongitude,

        double dropoffLatitude,
        double dropoffLongitude,

        Instant startedAt,
        Instant completedAt
) {}