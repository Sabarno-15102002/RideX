package com.ridex.payment.service;

import java.util.UUID;

import com.ridex.payment.dto.PaymentMethod;
import com.ridex.payment.dto.response.FareQuoteResponse;
import com.ridex.payment.dto.response.PaymentResponse;
import com.ridex.payment.event.TripCompletedEvent;

public interface PaymentService {

    PaymentResponse createPendingPayment(TripCompletedEvent event, FareQuoteResponse fareQuote);

    PaymentResponse refund(UUID paymentId);

    PaymentResponse getPaymentForAuthenticatedRider(UUID tripId);

    PaymentResponse processPayment(UUID paymentId);

    PaymentResponse initiatePayment(UUID paymentId, PaymentMethod paymentMethod);

}
