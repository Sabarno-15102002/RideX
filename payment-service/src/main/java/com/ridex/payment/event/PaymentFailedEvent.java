package com.ridex.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        UUID paymentId,
        UUID tripId,
        UUID riderId,
        BigDecimal amount,
        String currency,
        Instant failedAt
) {}