package com.ridex.location.entity;

import java.time.Instant;
import java.util.UUID;

public record DriverLocation(
        UUID driverId,
        double latitude,
        double longitude,
        Instant updatedAt
) {
}