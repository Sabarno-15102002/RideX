package com.ridex.trip.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.ridex.trip.dto.TripStatus;

public record TripResponse(
        UUID id,
        UUID riderId,
        UUID driverId,
        double pickupLatitude,
        double pickupLongitude,
        double dropoffLatitude,
        double dropoffLongitude,
        TripStatus status,
        Instant requestedAt
) {}