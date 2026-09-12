package com.ridex.trip.event;

import java.time.Instant;
import java.util.UUID;

public record TripRequestedEvent(
        UUID eventId,
        UUID tripId,
        UUID riderId,
        double pickupLatitude,
        double pickupLongitude,
        double dropoffLatitude,
        double dropoffLongitude,
        Instant requestedAt
) {}