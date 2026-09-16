package com.ridex.trip.event.event;

import java.time.Instant;
import java.util.UUID;

public record DriverRideExpiredEvent(
        UUID eventId,
        UUID tripId,
        UUID driverId,
        Instant expiredAt
) {
}