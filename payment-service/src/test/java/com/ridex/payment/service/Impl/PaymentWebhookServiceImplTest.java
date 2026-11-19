package com.ridex.payment.service.Impl;

import com.ridex.payment.config.PaymentMetrics;
import com.ridex.payment.dto.PaymentAttemptStatus;
import com.ridex.payment.dto.PaymentProviderWebhook;
import com.ridex.payment.dto.PaymentStatus;
import com.ridex.payment.entity.Payment;
import com.ridex.payment.entity.PaymentAttempt;
import com.ridex.payment.entity.PaymentProviderEvent;
import com.ridex.payment.exception.IllegalOperationException;
import com.ridex.payment.exception.InvalidPaymentStateException;
import com.ridex.payment.repository.PaymentAttemptRepository;
import com.ridex.payment.repository.PaymentProviderEventRepository;
import com.ridex.payment.repository.PaymentRepository;
import com.ridex.payment.service.util.OutboxEventService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceImplTest {

    @Mock
    PaymentProviderEventRepository providerEventRepository;
    @Mock
    PaymentRepository paymentRepository;
    @Mock
    PaymentAttemptRepository attemptRepository;
    @Mock
    OutboxEventService outboxEventService;
    @Mock
    PaymentMetrics paymentMetrics;

    @InjectMocks
    PaymentWebhookServiceImpl service;

    UUID paymentId;
    UUID tripId;
    UUID riderId;
    Payment payment;
    PaymentAttempt attempt;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        tripId = UUID.randomUUID();
        riderId = UUID.randomUUID();

        payment = new Payment();
        payment.setId(paymentId);
        payment.setTripId(tripId);
        payment.setRiderId(riderId);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(Instant.now().minusSeconds(30));
        payment.setUpdatedAt(Instant.now());

        attempt = new PaymentAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setPaymentId(paymentId);
        attempt.setAttemptNumber(1);
        attempt.setIdempotencyKey("payment:" + paymentId + ":attempt:1");
        attempt.setProviderPaymentId("provider-123");
        attempt.setStatus(PaymentAttemptStatus.PROCESSING);
        attempt.setCreatedAt(Instant.now());
        attempt.setUpdatedAt(Instant.now());
    }

    private PaymentProviderWebhook webhook(String eventId, String status) {
        return new PaymentProviderWebhook(
                "provider-123", "idem-123", status, eventId, "declined");
    }

    private void arrange() {
        when(providerEventRepository.existsById(any())).thenReturn(false);
        when(attemptRepository.findByProviderPaymentIdForUpdate("provider-123"))
                .thenReturn(Optional.of(attempt));
        when(paymentRepository.findByIdForUpdate(paymentId))
                .thenReturn(Optional.of(payment));
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(providerEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void successfulWebhookUpdatesAttemptAndPaymentAndEmitsEvent() {
        arrange();

        service.process(webhook("evt-success", "SUCCESS"));

        assertEquals(PaymentAttemptStatus.SUCCESS, attempt.getStatus());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("provider-123", payment.getProviderPaymentId());

        verify(attemptRepository).save(attempt);
        verify(paymentRepository).save(payment);
        verify(outboxEventService).savePaymentSucceededEvent(any());
        verify(paymentMetrics).recordPaymentSucceeded();
        verify(providerEventRepository).save(any(PaymentProviderEvent.class));
    }

    @Test
    void failedWebhookUpdatesAttemptAndPayment() {
        arrange();

        service.process(webhook("evt-failed", "FAILED"));

        assertEquals(PaymentAttemptStatus.FAILED, attempt.getStatus());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("declined", attempt.getFailureReason());

        verify(attemptRepository).save(attempt);
        verify(paymentRepository).save(payment);
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void duplicateWebhookIsIgnored() {
        when(providerEventRepository.existsById("evt-duplicate")).thenReturn(true);

        service.process(webhook("evt-duplicate", "SUCCESS"));

        verifyNoInteractions(attemptRepository, paymentRepository, outboxEventService);
        verify(providerEventRepository, never()).save(any());
    }

    @Test
    void unknownProviderAttemptThrowsNotFound() {
        when(providerEventRepository.existsById(any())).thenReturn(false);
        when(attemptRepository.findByProviderPaymentIdForUpdate("provider-123"))
                .thenReturn(Optional.empty());

        assertThrows(org.apache.kafka.common.errors.ResourceNotFoundException.class,
                () -> service.process(webhook("evt-unknown", "SUCCESS")));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void unsupportedProviderStatusThrows() {
        when(providerEventRepository.existsById(any())).thenReturn(false);
        when(attemptRepository.findByProviderPaymentIdForUpdate("provider-123"))
                .thenReturn(Optional.of(attempt));

        assertThrows(IllegalOperationException.class,
                () -> service.process(webhook("evt-pending", "PENDING")));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void webhookCannotTransitionAlreadySuccessfulAttempt() {
        attempt.setStatus(PaymentAttemptStatus.SUCCESS);

        when(providerEventRepository.existsById(any())).thenReturn(false);
        when(attemptRepository.findByProviderPaymentIdForUpdate("provider-123"))
                .thenReturn(Optional.of(attempt));

        assertThrows(InvalidPaymentStateException.class,
                () -> service.process(webhook("evt-again", "FAILED")));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void webhookCannotTransitionAlreadyFailedPayment() {
        payment.setStatus(PaymentStatus.FAILED);

        when(providerEventRepository.existsById("evt-conflict"))
                .thenReturn(false);
        when(attemptRepository.findByProviderPaymentIdForUpdate("provider-123"))
                .thenReturn(Optional.of(attempt));
        when(paymentRepository.findByIdForUpdate(paymentId))
                .thenReturn(Optional.of(payment));

        assertThrows(InvalidPaymentStateException.class,
                () -> service.process(webhook("evt-conflict", "SUCCESS")));

        verify(paymentRepository, never()).save(any());
        verify(providerEventRepository, never()).save(any());
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void webhookCannotMapNonFinalAttemptState() {
        // The service maps only SUCCESS and FAILED provider statuses.
        when(providerEventRepository.existsById(any())).thenReturn(false);
        when(attemptRepository.findByProviderPaymentIdForUpdate("provider-123"))
                .thenReturn(Optional.of(attempt));

        assertThrows(IllegalOperationException.class,
                () -> service.process(webhook("evt-processing", "PROCESSING")));
    }
}
