package com.ridex.payment.service.provider;

import org.springframework.stereotype.Component;

import com.ridex.payment.service.PaymentWebhookVerifier;

@Component
public class MockPaymentWebhookVerifier implements PaymentWebhookVerifier {

    @Override
    public boolean verify(String payload, String signature) {

        return "mock-valid-signature".equals(signature);
    }
}