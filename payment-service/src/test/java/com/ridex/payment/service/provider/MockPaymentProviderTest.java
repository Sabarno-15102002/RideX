package com.ridex.payment.service.provider;

import com.ridex.payment.dto.response.PaymentResult;
import com.ridex.payment.entity.Payment;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MockPaymentProviderTest {

    @Test
    void createsSuccessfulPaymentResult() {
        Payment payment = new Payment();
        payment.setIdempotencyKey("payment-key");

        PaymentResult result = new MockPaymentProvider().createPayment(payment);

        assertTrue(result.successful());
        assertEquals("mock_payment-key", result.providerPaymentId());
        assertNull(result.failureReason());
    }

    @Test
    void createsSuccessfulRefundResult() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setId(paymentId);

        PaymentResult result = new MockPaymentProvider().refundPayment(payment);

        assertTrue(result.successful());
        assertEquals("mock_refund_" + paymentId, result.providerPaymentId());
        assertNull(result.failureReason());
    }
}
