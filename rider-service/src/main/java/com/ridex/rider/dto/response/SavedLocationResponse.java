package com.ridex.rider.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SavedLocationResponse(
        UUID id,
        String label,
        Double latitude,
        Double longitude,
        String address,
        Instant createdAt
) {
}