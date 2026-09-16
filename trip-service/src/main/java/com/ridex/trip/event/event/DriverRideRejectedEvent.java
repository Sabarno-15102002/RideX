package com.ridex.trip.event.event;

import java.time.Instant;
import java.util.UUID;

public record DriverRideRejectedEvent(
        UUID eventId,
        UUID tripId,
        UUID driverId,
        Instant rejectedAt
) {}