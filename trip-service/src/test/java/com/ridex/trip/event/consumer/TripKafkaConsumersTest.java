package com.ridex.trip.event.consumer;

import com.ridex.trip.entity.ProcessedEvent;
import com.ridex.trip.event.event.*;
import com.ridex.trip.repository.ProcessedEventRepository;
import com.ridex.trip.service.RiderIdentityService;
import com.ridex.trip.service.TripPaymentProjection;
import com.ridex.trip.service.TripService;
import com.ridex.trip.service.util.TripPaymentQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripKafkaConsumersTest {
    @Mock TripService tripService;
    @Mock RiderIdentityService riderIdentityService;
    @Mock TripPaymentProjection projection;
    @Mock ProcessedEventRepository processedEventRepository;
    @Mock TripPaymentQueryService tripPaymentQueryService;

    UUID eventId, tripId, riderId, driverId, paymentId, userId;
    Instant now;

    @BeforeEach void setUp() {
        eventId = UUID.randomUUID(); tripId = UUID.randomUUID();
        riderId = UUID.randomUUID(); driverId = UUID.randomUUID();
        paymentId = UUID.randomUUID(); userId = UUID.randomUUID();
        now = Instant.parse("2026-11-10T10:00:00Z");
    }

    @Test void driverMatchRequestedConsumer_shouldDelegateEvent() {
        DriverMatchRequestedEvent event = new DriverMatchRequestedEvent(eventId, tripId, driverId, now);
        new DriverMatchRequestedConsumer(tripService).consume(event);
        verify(tripService).assignDriver(event);
    }

    @Test void driverRideAcceptedConsumer_shouldDelegateEvent() {
        DriverRideAcceptedEvent event = new DriverRideAcceptedEvent(eventId, tripId, driverId, now);
        new DriverRideAcceptedConsumer(tripService).consume(event);
        verify(tripService).handleDriverRideAccepted(event);
    }

    @Test void driverRideRejectedConsumer_shouldDelegateEvent() {
        DriverRideRejectedEvent event = new DriverRideRejectedEvent(eventId, tripId, driverId, now);
        new DriverRideRejectedConsumer(tripService).consume(event);
        verify(tripService).handleDriverRideRejected(event);
    }

    @Test void driverRideExpiredConsumer_shouldDelegateEvent() {
        DriverRideExpiredEvent event = new DriverRideExpiredEvent(eventId, tripId, driverId, now);
        new DriverRideExpiredConsumer(tripService).consume(event);
        verify(tripService).handleDriverRideExpired(event);
    }

    @Test void driverArrivedConsumer_shouldDelegateEvent() {
        DriverArrivedEvent event = new DriverArrivedEvent(eventId, tripId, driverId, now);
        new DriverArrivedConsumer(tripService).consume(event);
        verify(tripService).handleDriverArrived(event);
    }

    @Test void driverTripStartedConsumer_shouldDelegateEvent() {
        DriverTripStartedEvent event = new DriverTripStartedEvent(eventId, tripId, driverId, now);
        new DriverTripStartedConsumer(tripService).consume(event);
        verify(tripService).handleDriverTripStarted(event);
    }

    @Test void driverTripCompletedConsumer_shouldDelegateEvent() {
        DriverTripCompletedEvent event = new DriverTripCompletedEvent(eventId, tripId, driverId, now);
        new DriverTripCompletedConsumer(tripService).consume(event);
        verify(tripService).handleDriverTripCompleted(event);
    }

    @Test void riderCreatedConsumer_shouldDelegateEvent() {
        RiderCreatedEvent event = new RiderCreatedEvent(eventId, riderId, userId, now);
        new RiderCreatedConsumer(riderIdentityService).consume(event);
        verify(riderIdentityService).handleRiderCreated(event);
    }

    @Test void paymentSucceededConsumer_shouldProjectAndRecordEvent() {
        PaymentSucceededEvent event = new PaymentSucceededEvent(eventId, paymentId, tripId, riderId,
                new BigDecimal("250.00"), "INR", now);
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        new PaymentEventConsumer(projection, processedEventRepository, tripPaymentQueryService)
                .handlePaymentSucceeded(event);
        verify(projection).project(event);
        verify(processedEventRepository).save(argThat(p -> p.getEventId().equals(eventId)));
    }

    @Test void paymentSucceededConsumer_shouldIgnoreDuplicateEvent() {
        PaymentSucceededEvent event = new PaymentSucceededEvent(eventId, paymentId, tripId, riderId,
                new BigDecimal("250.00"), "INR", now);
        when(processedEventRepository.existsById(eventId)).thenReturn(true);
        new PaymentEventConsumer(projection, processedEventRepository, tripPaymentQueryService)
                .handlePaymentSucceeded(event);
        verifyNoInteractions(projection);
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }

    @Test void paymentFailedConsumer_shouldProjectAndRecordEvent() {
        PaymentFailedEvent event = new PaymentFailedEvent(eventId, paymentId, tripId, riderId,
                new BigDecimal("250.00"), "INR", now);
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        new PaymentEventConsumer(projection, processedEventRepository, tripPaymentQueryService)
                .handlePaymentFailed(event);
        verify(projection).project(event);
        verify(processedEventRepository).save(argThat(p -> p.getEventId().equals(eventId)));
    }

    @Test void paymentFailedConsumer_shouldIgnoreDuplicateEvent() {
        PaymentFailedEvent event = new PaymentFailedEvent(eventId, paymentId, tripId, riderId,
                new BigDecimal("250.00"), "INR", now);
        when(processedEventRepository.existsById(eventId)).thenReturn(true);
        new PaymentEventConsumer(projection, processedEventRepository, tripPaymentQueryService)
                .handlePaymentFailed(event);
        verifyNoInteractions(projection);
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }

    @Test void paymentRefundedConsumer_shouldProjectAndRecordEvent() {
        PaymentRefundedEvent event = new PaymentRefundedEvent(eventId, paymentId, tripId, riderId,
                new BigDecimal("250.00"), "INR", now);
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        new PaymentEventConsumer(projection, processedEventRepository, tripPaymentQueryService)
                .handlePaymentRefunded(event);
        verify(projection).project(event);
        verify(processedEventRepository).save(argThat(p -> p.getEventId().equals(eventId)));
    }

    @Test void paymentRefundedConsumer_shouldIgnoreDuplicateEvent() {
        PaymentRefundedEvent event = new PaymentRefundedEvent(eventId, paymentId, tripId, riderId,
                new BigDecimal("250.00"), "INR", now);
        when(processedEventRepository.existsById(eventId)).thenReturn(true);
        new PaymentEventConsumer(projection, processedEventRepository, tripPaymentQueryService)
                .handlePaymentRefunded(event);
        verifyNoInteractions(projection);
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }

    @Test void paymentCreatedConsumer_shouldCreateProjectionAndRecordEvent() {
        PaymentCreatedEvent event = new PaymentCreatedEvent(eventId, paymentId, tripId, riderId,
                new BigDecimal("250.00"), "INR", now);
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        new PaymentEventConsumer(projection, processedEventRepository, tripPaymentQueryService)
                .handlePaymentCreated(event);
        verify(tripPaymentQueryService).createPaymentForTrip(event);
        verify(processedEventRepository).save(argThat(p -> p.getEventId().equals(eventId)));
    }

    @Test void paymentCreatedConsumer_shouldIgnoreDuplicateEvent() {
        PaymentCreatedEvent event = new PaymentCreatedEvent(eventId, paymentId, tripId, riderId,
                new BigDecimal("250.00"), "INR", now);
        when(processedEventRepository.existsById(eventId)).thenReturn(true);
        new PaymentEventConsumer(projection, processedEventRepository, tripPaymentQueryService)
                .handlePaymentCreated(event);
        verifyNoInteractions(tripPaymentQueryService);
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }
}
