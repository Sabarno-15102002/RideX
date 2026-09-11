package com.ridex.location.dto.response;

import java.util.UUID;

public record NearbyDriverResponse(
        UUID driverId,
        double distanceKm
) {
}