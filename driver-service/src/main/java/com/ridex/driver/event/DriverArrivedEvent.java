package com.ridex.driver.event;

import java.time.Instant;
import java.util.UUID;

public record DriverArrivedEvent(
        UUID eventId,
        UUID tripId,
        UUID driverId,
        Instant arrivedAt
) {
}
