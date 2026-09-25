package com.ridex.payment.service;

import com.ridex.payment.dto.response.PaymentResult;
import com.ridex.payment.entity.Payment;

public interface PaymentProvider {

    PaymentResult createPayment(Payment payment);

    PaymentResult refundPayment(Payment payment);
}