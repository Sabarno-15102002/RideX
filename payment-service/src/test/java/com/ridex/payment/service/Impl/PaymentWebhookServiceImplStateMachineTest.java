package com.ridex.payment.service.Impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ridex.payment.config.PaymentMetrics;
import com.ridex.payment.dto.PaymentAttemptStatus;
import com.ridex.payment.dto.PaymentStatus;
import com.ridex.payment.exception.IllegalOperationException;
import com.ridex.payment.exception.InvalidPaymentStateException;
import com.ridex.payment.repository.PaymentAttemptRepository;
import com.ridex.payment.repository.PaymentProviderEventRepository;
import com.ridex.payment.repository.PaymentRepository;
import com.ridex.payment.service.util.OutboxEventService;

class PaymentWebhookServiceImplStateMachineTest {

    private PaymentWebhookServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentWebhookServiceImpl(
                mock(PaymentProviderEventRepository.class),
                mock(PaymentRepository.class),
                mock(PaymentAttemptRepository.class),
                mock(OutboxEventService.class),
                mock(PaymentMetrics.class));
    }

    // ---------------------------------------------------------
    // mapAttemptStatus()
    // ---------------------------------------------------------

    @Test
    void shouldMapSuccessAttemptStatus() {
        PaymentAttemptStatus result = ReflectionTestUtils.invokeMethod(
                service,
                "mapAttemptStatus",
                "SUCCESS");

        assertEquals(PaymentAttemptStatus.SUCCESS, result);
    }

    @Test
    void shouldMapFailedAttemptStatus() {
        PaymentAttemptStatus result = ReflectionTestUtils.invokeMethod(
                service,
                "mapAttemptStatus",
                "FAILED");

        assertEquals(PaymentAttemptStatus.FAILED, result);
    }

    @Test
    void shouldRejectUnsupportedAttemptStatus() {
        assertThrows(
                IllegalOperationException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "mapAttemptStatus",
                        "PENDING"));
    }

    @Test
    void shouldMapAttemptStatusWithoutCaseSensitivity() {
        PaymentAttemptStatus result = ReflectionTestUtils.invokeMethod(
                service,
                "mapAttemptStatus",
                "success");

        assertEquals(PaymentAttemptStatus.SUCCESS, result);
    }

    // ---------------------------------------------------------
    // mapPaymentStatus()
    // ---------------------------------------------------------

    @Test
    void shouldMapSuccessfulAttemptToSuccessfulPayment() {
        PaymentStatus result = ReflectionTestUtils.invokeMethod(
                service,
                "mapPaymentStatus",
                PaymentAttemptStatus.SUCCESS);

        assertEquals(PaymentStatus.SUCCESS, result);
    }

    @Test
    void shouldMapFailedAttemptToFailedPayment() {
        PaymentStatus result = ReflectionTestUtils.invokeMethod(
                service,
                "mapPaymentStatus",
                PaymentAttemptStatus.FAILED);

        assertEquals(PaymentStatus.FAILED, result);
    }

    @Test
    void shouldRejectCreatedAttemptAsFinalPaymentStatus() {
        assertThrows(
                IllegalOperationException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "mapPaymentStatus",
                        PaymentAttemptStatus.CREATED));
    }

    @Test
    void shouldRejectProcessingAttemptAsFinalPaymentStatus() {
        assertThrows(
                IllegalOperationException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "mapPaymentStatus",
                        PaymentAttemptStatus.PROCESSING));
    }

    // ---------------------------------------------------------
    // validateAttemptTransition()
    // ---------------------------------------------------------

    @Test
    void createdAttemptShouldAllowProcessing() {
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                service,
                "validateAttemptTransition",
                PaymentAttemptStatus.CREATED,
                PaymentAttemptStatus.PROCESSING));
    }

    @Test
    void createdAttemptShouldRejectSuccess() {
        assertThrows(
                InvalidPaymentStateException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "validateAttemptTransition",
                        PaymentAttemptStatus.CREATED,
                        PaymentAttemptStatus.SUCCESS));
    }

    @Test
    void createdAttemptShouldRejectFailure() {
        assertThrows(
                InvalidPaymentStateException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "validateAttemptTransition",
                        PaymentAttemptStatus.CREATED,
                        PaymentAttemptStatus.FAILED));
    }

    @Test
    void processingAttemptShouldAllowSuccess() {
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                service,
                "validateAttemptTransition",
                PaymentAttemptStatus.PROCESSING,
                PaymentAttemptStatus.SUCCESS));
    }

    @Test
    void processingAttemptShouldAllowFailure() {
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                service,
                "validateAttemptTransition",
                PaymentAttemptStatus.PROCESSING,
                PaymentAttemptStatus.FAILED));
    }

    @Test
    void processingAttemptShouldRejectCreated() {
        assertThrows(
                InvalidPaymentStateException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "validateAttemptTransition",
                        PaymentAttemptStatus.PROCESSING,
                        PaymentAttemptStatus.CREATED));
    }

    @Test
    void successfulAttemptShouldRejectFurtherTransition() {
        assertThrows(
                InvalidPaymentStateException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "validateAttemptTransition",
                        PaymentAttemptStatus.SUCCESS,
                        PaymentAttemptStatus.FAILED));
    }

    @Test
    void failedAttemptShouldRejectFurtherTransition() {
        assertThrows(
                InvalidPaymentStateException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "validateAttemptTransition",
                        PaymentAttemptStatus.FAILED,
                        PaymentAttemptStatus.SUCCESS));
    }

    // ---------------------------------------------------------
    // validatePaymentTransition()
    // ---------------------------------------------------------

    @Test
    void pendingPaymentShouldAllowSuccess() {
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                service,
                "validatePaymentTransition",
                PaymentStatus.PENDING,
                PaymentStatus.SUCCESS));
    }

    @Test
    void pendingPaymentShouldAllowFailure() {
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                service,
                "validatePaymentTransition",
                PaymentStatus.PENDING,
                PaymentStatus.FAILED));
    }

    @Test
    void pendingPaymentShouldRejectPending() {
        assertThrows(
                InvalidPaymentStateException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "validatePaymentTransition",
                        PaymentStatus.PENDING,
                        PaymentStatus.PENDING));
    }

    @Test
    void successfulPaymentShouldRejectFurtherTransition() {
        assertThrows(
                InvalidPaymentStateException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "validatePaymentTransition",
                        PaymentStatus.SUCCESS,
                        PaymentStatus.FAILED));
    }

    @Test
    void failedPaymentShouldRejectFurtherTransition() {
        assertThrows(
                InvalidPaymentStateException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "validatePaymentTransition",
                        PaymentStatus.FAILED,
                        PaymentStatus.SUCCESS));
    }

    @Test
    void refundedPaymentShouldRejectFurtherTransition() {
        assertThrows(
                InvalidPaymentStateException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "validatePaymentTransition",
                        PaymentStatus.REFUNDED,
                        PaymentStatus.SUCCESS));
    }
}
