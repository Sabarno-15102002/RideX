package com.ridex.payment.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record FareQuoteResponse(
        UUID tripId,
        UUID riderId,
        BigDecimal baseFare,
        BigDecimal distanceKm,
        BigDecimal durationMinutes,
        BigDecimal distanceFare,
        BigDecimal durationFare,
        BigDecimal totalFare,
        String currency
) {
}