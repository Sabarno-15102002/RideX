
package com.ridex.trip.entity;

import com.ridex.trip.dto.PaymentStatus;
import com.ridex.trip.dto.TripStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityLifecycleTest {

    @Test
    void tripOnCreate_shouldPopulateDefaultValues() {
        Trip trip = new Trip();

        trip.onCreate();

        assertNotNull(trip.getCreatedAt());
        assertNotNull(trip.getUpdatedAt());
        assertNotNull(trip.getRequestedAt());
        assertEquals(TripStatus.REQUESTED, trip.getStatus());
        assertEquals(trip.getCreatedAt(), trip.getUpdatedAt());
    }

    @Test
    void tripOnCreate_shouldPreserveExistingValues() {
        Instant requestedAt = Instant.parse("2026-01-01T10:00:00Z");

        Trip trip = Trip.builder()
                .requestedAt(requestedAt)
                .status(TripStatus.MATCHING)
                .build();

        trip.onCreate();

        assertEquals(requestedAt, trip.getRequestedAt());
        assertEquals(TripStatus.MATCHING, trip.getStatus());
        assertNotNull(trip.getCreatedAt());
        assertNotNull(trip.getUpdatedAt());
    }

    @Test
    void tripOnUpdate_shouldRefreshUpdatedAt() throws InterruptedException {
        Instant previous = Instant.parse("2020-01-01T00:00:00Z");

        Trip trip = Trip.builder()
                .updatedAt(previous)
                .build();

        trip.onUpdate();

        assertTrue(trip.getUpdatedAt().isAfter(previous));
    }

    @Test
    void outboxEventOnCreate_shouldPopulateCreatedAtWhenNull() {
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("TRIP")
                .aggregateId(UUID.randomUUID())
                .eventType("trip.requested")
                .payload("{}")
                .build();

        event.onCreate();

        assertNotNull(event.getCreatedAt());
    }

    @Test
    void outboxEventOnCreate_shouldPreserveExistingCreatedAt() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");

        OutboxEvent event = OutboxEvent.builder()
                .createdAt(createdAt)
                .build();

        event.onCreate();

        assertEquals(createdAt, event.getCreatedAt());
    }

    @Test
    void processedEventOnCreate_shouldPopulateProcessedAtWhenNull() {
        ProcessedEvent event = ProcessedEvent.builder()
                .eventId(UUID.randomUUID())
                .build();

        event.onCreate();

        assertNotNull(event.getProcessedAt());
    }

    @Test
    void processedEventOnCreate_shouldPreserveExistingProcessedAt() {
        Instant processedAt = Instant.parse("2026-01-01T10:00:00Z");

        ProcessedEvent event = ProcessedEvent.builder()
                .processedAt(processedAt)
                .build();

        event.onCreate();

        assertEquals(processedAt, event.getProcessedAt());
    }

    @Test
    void riderIdentityOnCreate_shouldPopulateCreatedAtWhenNull() {
        RiderIdentity identity = RiderIdentity.builder()
                .userId(UUID.randomUUID())
                .riderId(UUID.randomUUID())
                .build();

        identity.onCreate();

        assertNotNull(identity.getCreatedAt());
    }

    @Test
    void riderIdentityOnCreate_shouldPreserveExistingCreatedAt() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");

        RiderIdentity identity = RiderIdentity.builder()
                .createdAt(createdAt)
                .build();

        identity.onCreate();

        assertEquals(createdAt, identity.getCreatedAt());
    }

    @Test
    void tripPaymentConstructor_shouldInitializeAllFields() {
        UUID tripId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        TripPayment payment = new TripPayment(
                tripId,
                paymentId,
                riderId,
                new BigDecimal("250.00"),
                "INR",
                PaymentStatus.PENDING,
                createdAt);

        assertEquals(tripId, payment.getTripId());
        assertEquals(paymentId, payment.getPaymentId());
        assertEquals(riderId, payment.getRiderId());
        assertEquals(new BigDecimal("250.00"), payment.getAmount());
        assertEquals("INR", payment.getCurrency());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(createdAt, payment.getCreatedAt());
        assertEquals(createdAt, payment.getUpdatedAt());
    }

    @Test
    void tripPaymentMarkSuccess_shouldUpdatePaymentState() {
        Instant createdAt = Instant.now();
        Instant paidAt = createdAt.plusSeconds(60);

        TripPayment payment = createPayment(createdAt);

        payment.markSuccess(paidAt);

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(paidAt, payment.getPaidAt());
        assertEquals(paidAt, payment.getUpdatedAt());
    }

    @Test
    void tripPaymentMarkFailed_shouldUpdatePaymentState() {
        Instant createdAt = Instant.now();
        Instant failedAt = createdAt.plusSeconds(60);

        TripPayment payment = createPayment(createdAt);

        payment.markFailed(failedAt);

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(failedAt, payment.getUpdatedAt());
        assertNull(payment.getPaidAt());
    }

    @Test
    void tripPaymentMarkRefunded_shouldUpdatePaymentState() {
        Instant createdAt = Instant.now();
        Instant refundedAt = createdAt.plusSeconds(120);

        TripPayment payment = createPayment(createdAt);

        payment.markRefunded(refundedAt);

        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals(refundedAt, payment.getRefundedAt());
        assertEquals(refundedAt, payment.getUpdatedAt());
    }

    private TripPayment createPayment(Instant createdAt) {
        return new TripPayment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("250.00"),
                "INR",
                PaymentStatus.PENDING,
                createdAt);
    }
}
