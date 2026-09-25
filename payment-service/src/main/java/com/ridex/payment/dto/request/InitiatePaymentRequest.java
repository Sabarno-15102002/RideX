package com.ridex.payment.dto.request;

import com.ridex.payment.dto.PaymentMethod;

import jakarta.validation.constraints.NotNull;

public record InitiatePaymentRequest(
        @NotNull
        PaymentMethod paymentMethod
) {
}