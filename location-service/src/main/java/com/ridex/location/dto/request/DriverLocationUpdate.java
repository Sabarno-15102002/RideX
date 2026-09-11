package com.ridex.location.dto.request;

import java.time.Instant;
import java.util.UUID;

public record DriverLocationUpdate(
        UUID driverId,
        double latitude,
        double longitude,
        Instant updatedAt
) {}