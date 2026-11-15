
package com.ridex.trip.service.util;

import com.ridex.trip.dto.PaymentStatus;
import com.ridex.trip.dto.response.TripPaymentResponse;
import com.ridex.trip.entity.TripPayment;
import com.ridex.trip.event.event.PaymentCreatedEvent;
import com.ridex.trip.exception.IllegalOperationException;
import com.ridex.trip.repository.TripPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripPaymentQueryServiceTest {

    @Mock
    private TripPaymentRepository repository;

    @InjectMocks
    private TripPaymentQueryService service;

    private UUID tripId;
    private UUID paymentId;
    private UUID riderId;
    private BigDecimal amount;
    private Instant createdAt;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        riderId = UUID.randomUUID();
        amount = new BigDecimal("250.00");
        createdAt = Instant.parse("2026-11-10T10:00:00Z");
    }

    private TripPayment createPayment() {
        return new TripPayment(
                tripId,
                paymentId,
                riderId,
                amount,
                "INR",
                PaymentStatus.PENDING,
                createdAt
        );
    }

    // ---------------------------------------------
    // GET PAYMENT FOR TRIP
    // ---------------------------------------------

    @Test
    void getPaymentForTrip_shouldReturnPaymentWhenRiderOwnsTrip() {

        TripPayment payment = createPayment();

        when(repository.findById(tripId))
                .thenReturn(Optional.of(payment));

        TripPaymentResponse response =
                service.getPaymentForTrip(tripId, riderId);

        assertNotNull(response);
        assertEquals(tripId, response.tripId());
        assertEquals(paymentId, response.paymentId());
        assertEquals(amount, response.amount());
        assertEquals("INR", response.currency());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertNull(response.paidAt());
        assertNull(response.refundedAt());

        verify(repository).findById(tripId);
    }

    @Test
    void getPaymentForTrip_shouldRejectDifferentRider() {

        TripPayment payment = createPayment();
        UUID differentRiderId = UUID.randomUUID();

        when(repository.findById(tripId))
                .thenReturn(Optional.of(payment));

        assertThrows(
                IllegalOperationException.class,
                () -> service.getPaymentForTrip(
                        tripId,
                        differentRiderId
                )
        );

        verify(repository).findById(tripId);
    }

    @Test
    void getPaymentForTrip_shouldThrowWhenPaymentDoesNotExist() {

        when(repository.findById(tripId))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getPaymentForTrip(tripId, riderId)
        );

        verify(repository).findById(tripId);
    }

    // ---------------------------------------------
    // CREATE PAYMENT FOR TRIP
    // ---------------------------------------------

    @Test
    void createPaymentForTrip_shouldSavePendingPayment() {

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                UUID.randomUUID(),
                paymentId,
                tripId,
                riderId,
                amount,
                "INR",
                createdAt
        );

        service.createPaymentForTrip(event);

        ArgumentCaptor<TripPayment> captor =
                ArgumentCaptor.forClass(TripPayment.class);

        verify(repository).save(captor.capture());

        TripPayment savedPayment = captor.getValue();

        assertEquals(tripId, savedPayment.getTripId());
        assertEquals(paymentId, savedPayment.getPaymentId());
        assertEquals(riderId, savedPayment.getRiderId());
        assertEquals(amount, savedPayment.getAmount());
        assertEquals("INR", savedPayment.getCurrency());
        assertEquals(PaymentStatus.PENDING, savedPayment.getStatus());
        assertEquals(createdAt, savedPayment.getCreatedAt());
        assertEquals(createdAt, savedPayment.getUpdatedAt());
    }

    @Test
    void createPaymentForTrip_shouldNotSaveWhenRepositoryFails() {

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                UUID.randomUUID(),
                paymentId,
                tripId,
                riderId,
                amount,
                "INR",
                createdAt
        );

        RuntimeException exception =
                new RuntimeException("Database unavailable");

        doThrow(exception).when(repository).save(any(TripPayment.class));

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.createPaymentForTrip(event)
        );

        assertEquals("Database unavailable", thrown.getMessage());

        verify(repository).save(any(TripPayment.class));
    }
}
