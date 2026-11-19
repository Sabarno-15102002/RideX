package com.ridex.payment.service.Impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ridex.payment.config.PaymentMetrics;
import com.ridex.payment.dto.PaymentAttemptStatus;
import com.ridex.payment.dto.PaymentProviderWebhook;
import com.ridex.payment.entity.PaymentAttempt;
import com.ridex.payment.repository.PaymentAttemptRepository;
import com.ridex.payment.repository.PaymentProviderEventRepository;
import com.ridex.payment.repository.PaymentRepository;
import com.ridex.payment.service.util.OutboxEventService;

class PaymentWebhookServiceImplMissingPaymentTest {

    private PaymentProviderEventRepository providerEventRepository;
    private PaymentRepository paymentRepository;
    private PaymentAttemptRepository paymentAttemptRepository;
    private PaymentWebhookServiceImpl service;

    @BeforeEach
    void setUp() {
        providerEventRepository = mock(PaymentProviderEventRepository.class);

        paymentRepository = mock(PaymentRepository.class);

        paymentAttemptRepository = mock(PaymentAttemptRepository.class);

        service = new PaymentWebhookServiceImpl(
                providerEventRepository,
                paymentRepository,
                paymentAttemptRepository,
                mock(OutboxEventService.class),
                mock(PaymentMetrics.class));
    }

    @Test
    void shouldThrowWhenParentPaymentDoesNotExist() {
        UUID paymentId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        PaymentAttempt attempt = new PaymentAttempt(
                attemptId,
                paymentId,
                1,
                "payment-idempotency-key",
                PaymentAttemptStatus.PROCESSING,
                Instant.now(),
                Instant.now());

        attempt.setProviderPaymentId("provider-payment-123");

        PaymentProviderWebhook webhook = new PaymentProviderWebhook(
                "provider-payment-123",
                "payment-idempotency-key",
                "SUCCESS",
                "provider-event-123",
                null);

        when(providerEventRepository.existsById("provider-event-123"))
                .thenReturn(false);

        when(paymentAttemptRepository.findByProviderPaymentIdForUpdate(
                "provider-payment-123"))
                .thenReturn(Optional.of(attempt));

        when(paymentRepository.findByIdForUpdate(paymentId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.process(webhook));

        assertEquals(
                "Payment not found: " + paymentId,
                exception.getMessage());

        verify(paymentAttemptRepository)
                .findByProviderPaymentIdForUpdate("provider-payment-123");

        verify(paymentRepository).findByIdForUpdate(paymentId);

        verify(paymentRepository, never()).save(any());
        verify(paymentAttemptRepository, never()).save(any());
    }
}
