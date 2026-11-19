package com.ridex.payment.service.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MockPaymentWebhookVerifierTest {

    private final MockPaymentWebhookVerifier verifier = new MockPaymentWebhookVerifier();

    @Test
    void acceptsMockValidSignature() {
        assertTrue(verifier.verify("{\"status\":\"SUCCESS\"}", "mock-valid-signature"));
    }

    @Test
    void rejectsInvalidSignature() {
        assertFalse(verifier.verify("payload", "invalid-signature"));
    }

    @Test
    void rejectsNullSignature() {
        assertFalse(verifier.verify("payload", null));
    }
}
