package com.ridex.payment.dto.response;

public record PaymentResult(
        boolean successful,
        String providerPaymentId,
        String failureReason
) {
}