package com.ridex.matching.dto.response;

import java.util.UUID;

public record NearbyDriverResponse(
        UUID driverId,
        double distanceKm
) {}