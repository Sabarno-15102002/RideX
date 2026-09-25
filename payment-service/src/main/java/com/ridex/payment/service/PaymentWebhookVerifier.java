package com.ridex.payment.service;

public interface PaymentWebhookVerifier {

    boolean verify(
            String payload,
            String signature
    );
}