package com.ridex.matching.event;

import java.time.Instant;
import java.util.UUID;

public record TripRematchingEvent(
        UUID eventId,
        UUID tripId,
        UUID riderId,
        UUID rejectedDriverId,
        double pickupLatitude,
        double pickupLongitude,
        double dropoffLatitude,
        double dropoffLongitude,
        Instant rematchingAt
) {
}
