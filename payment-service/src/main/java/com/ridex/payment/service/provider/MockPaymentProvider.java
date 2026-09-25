package com.ridex.payment.service.provider;

import org.springframework.stereotype.Component;

import com.ridex.payment.dto.response.PaymentResult;
import com.ridex.payment.entity.Payment;
import com.ridex.payment.service.PaymentProvider;

@Component
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public PaymentResult createPayment(Payment payment) {

        return new PaymentResult(
                true,
                "mock_" + payment.getIdempotencyKey(),
                null
        );
    }

    @Override
    public PaymentResult refundPayment(Payment payment) {

        return new PaymentResult(
                true,
                "mock_refund_" + payment.getId(),
                null
        );
    }
}