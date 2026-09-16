package com.ridex.trip.event.event;

import java.time.Instant;
import java.util.UUID;

public record DriverRideAcceptedEvent(
        UUID eventId,
        UUID tripId,
        UUID driverId,
        Instant acceptedAt
) {}