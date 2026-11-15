
package com.ridex.trip.service;

import com.ridex.trip.dto.PaymentStatus;
import com.ridex.trip.entity.TripPayment;
import com.ridex.trip.event.event.PaymentFailedEvent;
import com.ridex.trip.event.event.PaymentRefundedEvent;
import com.ridex.trip.event.event.PaymentSucceededEvent;
import com.ridex.trip.exception.IllegalOperationException;
import com.ridex.trip.exception.ResourceNotFoundException;
import com.ridex.trip.repository.TripPaymentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
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
class TripPaymentProjectionTest {

        @Mock
        private TripPaymentRepository tripPaymentRepository;

        @InjectMocks
        private TripPaymentProjection tripPaymentProjection;

        private final UUID tripId = UUID.randomUUID();
        private final UUID paymentId = UUID.randomUUID();
        private final UUID riderId = UUID.randomUUID();

        private final BigDecimal amount = new BigDecimal("250.00");
        private final String currency = "INR";

        private final Instant createdAt = Instant.parse("2026-11-10T10:00:00Z");

        private final Instant updatedAt = Instant.parse("2026-11-10T10:30:00Z");

        private TripPayment createPayment(PaymentStatus status) {
                return new TripPayment(
                                tripId,
                                paymentId,
                                riderId,
                                amount,
                                currency,
                                status,
                                createdAt);
        }

        // ------------------------------------------------
        // PAYMENT SUCCEEDED
        // ------------------------------------------------

        @Test
        void projectPaymentSucceeded_shouldCreateProjectionWhenNotPresent() {

                PaymentSucceededEvent event = new PaymentSucceededEvent(
                                UUID.randomUUID(),
                                paymentId,
                                tripId,
                                riderId,
                                amount,
                                currency,
                                updatedAt);

                when(tripPaymentRepository.findById(tripId))
                                .thenReturn(Optional.empty());

                tripPaymentProjection.project(event);

                ArgumentCaptor<TripPayment> captor = ArgumentCaptor.forClass(TripPayment.class);

                verify(tripPaymentRepository).save(captor.capture());

                TripPayment saved = captor.getValue();

                assertEquals(tripId, saved.getTripId());
                assertEquals(paymentId, saved.getPaymentId());
                assertEquals(riderId, saved.getRiderId());
                assertEquals(amount, saved.getAmount());
                assertEquals(currency, saved.getCurrency());
                assertEquals(PaymentStatus.SUCCESS, saved.getStatus());
                assertEquals(updatedAt, saved.getPaidAt());
        }

        @Test
        void projectPaymentSucceeded_shouldUpdatePendingPayment() {

                TripPayment payment = createPayment(PaymentStatus.PENDING);

                PaymentSucceededEvent event = new PaymentSucceededEvent(
                                UUID.randomUUID(),
                                paymentId,
                                tripId,
                                riderId,
                                amount,
                                currency,
                                updatedAt);

                when(tripPaymentRepository.findById(tripId))
                                .thenReturn(Optional.of(payment));

                tripPaymentProjection.project(event);

                assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
                assertEquals(updatedAt, payment.getPaidAt());
                assertEquals(updatedAt, payment.getUpdatedAt());

                verify(tripPaymentRepository).save(payment);
        }

        @Test
        void projectPaymentSucceeded_shouldIgnoreAlreadySuccessfulPayment() {

                TripPayment payment = createPayment(PaymentStatus.SUCCESS);

                PaymentSucceededEvent event = new PaymentSucceededEvent(
                                UUID.randomUUID(),
                                paymentId,
                                tripId,
                                riderId,
                                amount,
                                currency,
                                updatedAt);

                when(tripPaymentRepository.findById(tripId))
                                .thenReturn(Optional.of(payment));

                tripPaymentProjection.project(event);

                verify(tripPaymentRepository, never()).save(any());
        }

        @Test
        void projectPaymentSucceeded_shouldIgnoreRefundedPayment() {

                TripPayment payment = createPayment(PaymentStatus.REFUNDED);

                PaymentSucceededEvent event = new PaymentSucceededEvent(
                                UUID.randomUUID(),
                                paymentId,
                                tripId,
                                riderId,
                                amount,
                                currency,
                                updatedAt);

                when(tripPaymentRepository.findById(tripId))
                                .thenReturn(Optional.of(payment));

                tripPaymentProjection.project(event);

                verify(tripPaymentRepository, never()).save(any());
        }

        // ------------------------------------------------
        // PAYMENT FAILED
        // ------------------------------------------------

        @Test
        void projectPaymentFailed_shouldCreateFailedProjectionWhenNotPresent() {

                PaymentFailedEvent event = new PaymentFailedEvent(
                                UUID.randomUUID(),
                                paymentId,
                                tripId,
                                riderId,
                                amount,
                                currency,
                                updatedAt);

                when(tripPaymentRepository.findById(tripId))
                                .thenReturn(Optional.empty());

                tripPaymentProjection.project(event);

                ArgumentCaptor<TripPayment> captor = ArgumentCaptor.forClass(TripPayment.class);

                verify(tripPaymentRepository).save(captor.capture());

                TripPayment saved = captor.getValue();

                assertEquals(tripId, saved.getTripId());
                assertEquals(PaymentStatus.FAILED, saved.getStatus());
                assertEquals(updatedAt, saved.getUpdatedAt());
        }

        @Test
        void projectPaymentFailed_shouldUpdatePendingPayment() {

                TripPayment payment = createPayment(PaymentStatus.PENDING);

                PaymentFailedEvent event = new PaymentFailedEvent(
                                UUID.randomUUID(),
                                paymentId,
                                tripId,
                                riderId,
                                amount,
                                currency,
                                updatedAt);

                when(tripPaymentRepository.findById(tripId))
                                .thenReturn(Optional.of(payment));

                tripPaymentProjection.project(event);

                assertEquals(PaymentStatus.FAILED, payment.getStatus());
                assertEquals(updatedAt, payment.getUpdatedAt());

                verify(tripPaymentRepository).save(payment);
        }

        @Test
        void projectPaymentFailed_shouldIgnoreSuccessfulPayment() {

                TripPayment payment = createPayment(PaymentStatus.SUCCESS);

                PaymentFailedEvent event = new PaymentFailedEvent(
                                UUID.randomUUID(),
                                paymentId,
                                tripId,
                                riderId,
                                amount,
                                currency,
                                updatedAt);

                when(tripPaymentRepository.findById(tripId))
                                .thenReturn(Optional.of(payment));

                tripPaymentProjection.project(event);

                verify(tripPaymentRepository, never()).save(any());
        }

        @Test
        void projectPaymentFailed_shouldIgnoreRefundedPayment() {

                TripPayment payment = createPayment(PaymentStatus.REFUNDED);

                PaymentFailedEvent event = new PaymentFailedEvent(
                                UUID.randomUUID(),
                                paymentId,
                                tripId,
                                riderId,
                                amount,
                                currency,
                                updatedAt);

                when(tripPaymentRepository.findById(tripId))
                                .thenReturn(Optional.of(payment));

                tripPaymentProjection.project(event);

                verify(tripPaymentRepository, never()).save(any());
        }

        // ------------------------------------------------
        // PAYMENT REFUNDED
        // ------------------------------------------------

        @Test
        void projectPaymentRefunded_shouldRefundSuccessfulPayment() {

                TripPayment payment = createPayment(PaymentStatus.SUCCESS);

                PaymentRefundedEvent event = new PaymentRefundedEvent(
                                UUID.randomUUID(),
                                paymentId,
                                tripId,
                                riderId,
                                amount,
                                currency,
                                updatedAt);

                when(tripPaymentRepository.findById(tripId))
                                .thenReturn(Optional.of(payment));

                tripPaymentProjection.project(event);

                assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
                assertEquals(updatedAt, payment.getRefundedAt());
                assertEquals(updatedAt, payment.getUpdatedAt());

                verify(tripPaymentRepository).save(payment);
        }

        @Test
        void projectPaymentRefunded_shouldIgnoreAlreadyRefundedPayment() {

                TripPayment payment = createPayment(PaymentStatus.REFUNDED);

                PaymentRefundedEvent event = new PaymentRefundedEvent(
                                UUID.randomUUID(),
                                paymentId,
                                tripId,
                                riderId,
                                amount,
                                currency,
                                updatedAt);

                when(tripPaymentRepository.findById(tripId))
                                .thenReturn(Optional.of(payment));

                tripPaymentProjection.project(event);

                verify(tripPaymentRepository, never()).save(any());
        }

        @Test
        void projectPaymentRefunded_shouldThrowWhenPaymentDoesNotExist() {

                PaymentRefundedEvent event = new PaymentRefundedEvent(
                                UUID.randomUUID(),
                                paymentId,
                                tripId,
                                riderId,
                                amount,
                                currency,
                                updatedAt);

                when(tripPaymentRepository.findById(tripId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> tripPaymentProjection.project(event));

                verify(tripPaymentRepository, never()).save(any());
        }

        @Test
        void projectPaymentRefunded_shouldRejectPendingPayment() {

                TripPayment payment = createPayment(PaymentStatus.PENDING);

                PaymentRefundedEvent event = new PaymentRefundedEvent(
                                UUID.randomUUID(),
                                paymentId,
                                tripId,
                                riderId,
                                amount,
                                currency,
                                updatedAt);

                when(tripPaymentRepository.findById(tripId))
                                .thenReturn(Optional.of(payment));

                assertThrows(
                                IllegalOperationException.class,
                                () -> tripPaymentProjection.project(event));

                verify(tripPaymentRepository, never()).save(any());
        }

        @Test
        void projectPaymentRefunded_shouldRejectFailedPayment() {

                TripPayment payment = createPayment(PaymentStatus.FAILED);

                PaymentRefundedEvent event = new PaymentRefundedEvent(
                                UUID.randomUUID(),
                                paymentId,
                                tripId,
                                riderId,
                                amount,
                                currency,
                                updatedAt);

                when(tripPaymentRepository.findById(tripId))
                                .thenReturn(Optional.of(payment));

                assertThrows(
                                IllegalOperationException.class,
                                () -> tripPaymentProjection.project(event));

                verify(tripPaymentRepository, never()).save(any());
        }
}
