package com.ridex.trip.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ridex.trip.dto.PaymentStatus;

public record TripPaymentResponse(
        UUID tripId,
        UUID paymentId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant paidAt,
        Instant refundedAt
) {
}