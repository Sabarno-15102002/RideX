package com.ridex.trip.dto;

import java.math.BigDecimal;

public record PaymentSummary(
        BigDecimal amount,
        String currency,
        PaymentStatus status
) {
}