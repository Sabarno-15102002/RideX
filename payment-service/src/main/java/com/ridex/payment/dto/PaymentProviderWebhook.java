package com.ridex.payment.dto;

public record PaymentProviderWebhook(
        String providerPaymentId,
        String idempotencyKey,
        String status,
        String eventId,
        String failureReason
) {
}