package com.ridex.payment.service.Impl;

import com.ridex.payment.config.PaymentMetrics;
import com.ridex.payment.config.SecurityUtils;
import com.ridex.payment.dto.PaymentAttemptStatus;
import com.ridex.payment.dto.PaymentMethod;
import com.ridex.payment.dto.PaymentStatus;
import com.ridex.payment.dto.response.FareQuoteResponse;
import com.ridex.payment.dto.response.PaymentResult;
import com.ridex.payment.entity.Payment;
import com.ridex.payment.entity.PaymentAttempt;
import com.ridex.payment.event.TripCompletedEvent;
import com.ridex.payment.exception.InvalidPaymentStateException;
import com.ridex.payment.exception.PaymentProviderException;
import com.ridex.payment.exception.ResourceNotFoundException;
import com.ridex.payment.repository.PaymentAttemptRepository;
import com.ridex.payment.repository.PaymentRepository;
import com.ridex.payment.service.PaymentProvider;
import com.ridex.payment.service.RiderIdentityService;
import com.ridex.payment.service.provider.PaymentProviderFactory;
import com.ridex.payment.service.util.OutboxEventService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    PaymentRepository paymentRepository;
    @Mock
    RiderIdentityService riderIdentityService;
    @Mock
    PaymentProvider paymentProvider;
    @Mock
    PaymentProviderFactory paymentProviderFactory;
    @Mock
    PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    OutboxEventService outboxEventService;
    @Mock
    PaymentMetrics paymentMetrics;

    @InjectMocks
    PaymentServiceImpl service;

    UUID paymentId;
    UUID tripId;
    UUID riderId;
    UUID userId;
    Payment payment;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        tripId = UUID.randomUUID();
        riderId = UUID.randomUUID();
        userId = UUID.randomUUID();

        payment = payment(PaymentStatus.PENDING);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Payment payment(PaymentStatus status) {
        Payment p = new Payment();
        p.setId(paymentId);
        p.setTripId(tripId);
        p.setRiderId(riderId);
        p.setAmount(new BigDecimal("125.50"));
        p.setCurrency("INR");
        p.setStatus(status);
        p.setIdempotencyKey("trip:" + tripId);
        p.setCreatedAt(Instant.now().minusSeconds(60));
        p.setUpdatedAt(Instant.now().minusSeconds(60));
        return p;
    }

    private TripCompletedEvent tripEvent() {
        return new TripCompletedEvent(
                UUID.randomUUID(), tripId, riderId, UUID.randomUUID(),
                12.9, 77.5, 13.0, 77.6,
                Instant.now().minusSeconds(600), Instant.now());
    }

    private FareQuoteResponse fareQuote() {
        return new FareQuoteResponse(
                tripId, riderId,
                new BigDecimal("30.00"),
                new BigDecimal("5.00"),
                new BigDecimal("10.00"),
                new BigDecimal("50.00"),
                new BigDecimal("45.50"),
                new BigDecimal("125.50"),
                "INR");
    }

    @Test
    void createPendingPaymentCreatesPaymentAndOutboxEvent() {
        when(paymentRepository.findByTripId(tripId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment saved = inv.getArgument(0);
            saved.setId(paymentId);
            return saved;
        });

        var response = service.createPendingPayment(tripEvent(), fareQuote());

        assertEquals(paymentId, response.id());
        assertEquals(tripId, response.tripId());
        assertEquals(riderId, response.riderId());
        assertEquals(new BigDecimal("125.50"), response.amount());
        assertEquals("INR", response.currency());
        assertEquals(PaymentStatus.PENDING, response.status());

        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.PENDING
                && p.getPaymentMethod() == null
                && p.getIdempotencyKey().equals("trip:" + tripId)));
        verify(outboxEventService).savePaymentCreatedEvent(any());
    }

    @Test
    void createPendingPaymentReturnsExistingPaymentWithoutCreatingAnother() {
        Payment existing = payment(PaymentStatus.PENDING);
        when(paymentRepository.findByTripId(tripId)).thenReturn(Optional.of(existing));

        var response = service.createPendingPayment(tripEvent(), fareQuote());

        assertEquals(paymentId, response.id());
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void processPaymentSuccessUpdatesPaymentAndPublishesEvent() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentProvider.createPayment(payment))
                .thenReturn(new PaymentResult(true, "provider-123", null));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.processPayment(paymentId);

        assertEquals(PaymentStatus.SUCCESS, response.status());
        assertEquals("provider-123", response.providerPaymentId());
        verify(outboxEventService).savePaymentSucceededEvent(any());
        verify(paymentMetrics).recordPaymentSucceeded();
        verify(paymentRepository).save(payment);
    }

    @Test
    void processPaymentFailureUpdatesPaymentAndPublishesEvent() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentProvider.createPayment(payment))
                .thenReturn(new PaymentResult(false, null, "declined"));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.processPayment(paymentId);

        assertEquals(PaymentStatus.FAILED, response.status());
        verify(outboxEventService).savePaymentFailedEvent(any());
        verify(paymentMetrics).recordPaymentFailed();
    }

    @Test
    void processPaymentRejectsNonPendingPayment() {
        payment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThrows(InvalidPaymentStateException.class,
                () -> service.processPayment(paymentId));

        verify(paymentProvider, never()).createPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processPaymentRejectsUnknownPayment() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.processPayment(paymentId));
    }

    @Test
    void refundSuccessfulPaymentUpdatesStatusAndPublishesEvent() {
        payment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentProvider.refundPayment(payment))
                .thenReturn(new PaymentResult(true, "refund-123", null));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.refund(paymentId);

        assertEquals(PaymentStatus.REFUNDED, response.status());
        verify(outboxEventService).savePaymentRefundedEvent(any());
        verify(paymentMetrics).recordPaymentRefunded();
    }

    @Test
    void refundRejectsPaymentThatIsNotSuccessful() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThrows(InvalidPaymentStateException.class,
                () -> service.refund(paymentId));

        verify(paymentProvider, never()).refundPayment(any());
    }

    @Test
    void refundProviderFailureThrowsAndDoesNotSavePayment() {
        payment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentProvider.refundPayment(payment))
                .thenReturn(new PaymentResult(false, null, "refund rejected"));

        assertThrows(PaymentProviderException.class,
                () -> service.refund(paymentId));

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void getPaymentForAuthenticatedRiderReturnsRiderPayment() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(riderIdentityService.getRiderIdByUserId(userId)).thenReturn(riderId);
            when(paymentRepository.findByRiderIdAndTripId(riderId, tripId))
                    .thenReturn(Optional.of(payment));

            var response = service.getPaymentForAuthenticatedRider(tripId);

            assertEquals(paymentId, response.id());
            verify(paymentRepository).findByRiderIdAndTripId(riderId, tripId);
        }
    }

    @Test
    void getPaymentForAuthenticatedRiderThrowsWhenPaymentNotFound() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(riderIdentityService.getRiderIdByUserId(userId)).thenReturn(riderId);
            when(paymentRepository.findByRiderIdAndTripId(riderId, tripId))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getPaymentForAuthenticatedRider(tripId));
        }
    }

    @Test
    void initiatePaymentCreatesFirstAttemptAndStoresProviderReference() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(riderIdentityService.getRiderIdByUserId(userId)).thenReturn(riderId);
            when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
            when(paymentAttemptRepository.findTopByPaymentIdOrderByAttemptNumberDesc(paymentId))
                    .thenReturn(Optional.empty());
            when(paymentProviderFactory.getProvider()).thenReturn(paymentProvider);
            when(paymentProvider.createPayment(payment))
                    .thenReturn(new PaymentResult(true, "provider-abc", null));
            when(paymentAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var response = service.initiatePayment(paymentId, PaymentMethod.CARD);

            assertEquals(PaymentMethod.CARD, response.paymentMethod());
            assertEquals("provider-abc", response.providerPaymentId());
            verify(paymentAttemptRepository, times(3)).save(any(PaymentAttempt.class));
            ArgumentCaptor<PaymentAttempt> captor = ArgumentCaptor.forClass(PaymentAttempt.class);

            verify(paymentAttemptRepository, times(3)).save(captor.capture());

            assertTrue(captor.getAllValues().stream().anyMatch(a -> a.getAttemptNumber() == 1
                    && a.getIdempotencyKey().equals(
                            "payment:" + paymentId + ":attempt:1")
                    && a.getStatus() == PaymentAttemptStatus.PROCESSING));
        }
    }

    @Test
    void initiatePaymentIncrementsAttemptNumber() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            PaymentAttempt previous = new PaymentAttempt();
            previous.setAttemptNumber(2);

            security.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(riderIdentityService.getRiderIdByUserId(userId)).thenReturn(riderId);
            when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
            when(paymentAttemptRepository.findTopByPaymentIdOrderByAttemptNumberDesc(paymentId))
                    .thenReturn(Optional.of(previous));
            when(paymentProviderFactory.getProvider()).thenReturn(paymentProvider);
            when(paymentProvider.createPayment(payment))
                    .thenReturn(new PaymentResult(true, "provider-3", null));
            when(paymentAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.initiatePayment(paymentId, PaymentMethod.UPI);

            ArgumentCaptor<PaymentAttempt> captor = ArgumentCaptor.forClass(PaymentAttempt.class);

            verify(paymentAttemptRepository, times(3)).save(captor.capture());

            assertTrue(captor.getAllValues().stream().anyMatch(a -> a.getAttemptNumber() == 3
                    && a.getIdempotencyKey().endsWith(":attempt:3")));
        }
    }

    @Test
    void initiatePaymentRejectsPaymentOwnedByAnotherRider() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(riderIdentityService.getRiderIdByUserId(userId))
                    .thenReturn(UUID.randomUUID());
            when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));

            assertThrows(ResourceNotFoundException.class,
                    () -> service.initiatePayment(paymentId, PaymentMethod.CARD));

            verify(paymentProviderFactory, never()).getProvider();
        }
    }

    @Test
    void initiatePaymentRejectsNonPendingPayment() {
        payment.setStatus(PaymentStatus.FAILED);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(riderIdentityService.getRiderIdByUserId(userId)).thenReturn(riderId);
            when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));

            assertThrows(InvalidPaymentStateException.class,
                    () -> service.initiatePayment(paymentId, PaymentMethod.CARD));
        }
    }

    @Test
    void initiatePaymentThrowsWhenProviderDeclines() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(riderIdentityService.getRiderIdByUserId(userId)).thenReturn(riderId);
            when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
            when(paymentAttemptRepository.findTopByPaymentIdOrderByAttemptNumberDesc(paymentId))
                    .thenReturn(Optional.empty());
            when(paymentProviderFactory.getProvider()).thenReturn(paymentProvider);
            when(paymentProvider.createPayment(payment))
                    .thenReturn(new PaymentResult(false, null, "card declined"));
            when(paymentAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThrows(PaymentProviderException.class,
                    () -> service.initiatePayment(paymentId, PaymentMethod.CARD));

            ArgumentCaptor<PaymentAttempt> captor = ArgumentCaptor.forClass(PaymentAttempt.class);

            verify(paymentAttemptRepository, times(3)).save(captor.capture());

            assertTrue(captor.getAllValues().stream().anyMatch(a -> a.getStatus() == PaymentAttemptStatus.FAILED
                    && "card declined".equals(a.getFailureReason())));
            verify(paymentRepository, never()).save(any());
        }
    }

    @Test
    void initiatePaymentThrowsWhenPaymentDoesNotExist() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(riderIdentityService.getRiderIdByUserId(userId)).thenReturn(riderId);
            when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.initiatePayment(paymentId, PaymentMethod.CARD));
        }
    }
}
