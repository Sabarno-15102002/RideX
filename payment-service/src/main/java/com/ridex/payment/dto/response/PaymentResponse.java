package com.ridex.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ridex.payment.dto.PaymentMethod;
import com.ridex.payment.dto.PaymentStatus;

public record PaymentResponse(
        UUID id,
        UUID tripId,
        UUID riderId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        PaymentMethod paymentMethod,
        String providerPaymentId,
        Instant createdAt,
        Instant updatedAt
) {
}