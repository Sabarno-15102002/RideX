package com.ridex.rider.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record RiderResponse(
        UUID id,
        UUID userId,
        String name,
        BigDecimal rating
) {
}