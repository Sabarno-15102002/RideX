package com.ridex.driver.service.utilities;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;

import com.ridex.driver.entity.OutboxEvent;
import com.ridex.driver.event.*;
import com.ridex.driver.repository.OutboxEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private EventSerializer serializer;

    @InjectMocks
    private OutboxEventService outboxEventService;

    private UUID driverId;

    @BeforeEach
    void setUp() {
        driverId = UUID.randomUUID();

        when(serializer.serialize(any()))
                .thenReturn("{\"event\":\"test\"}");
    }

    @Test
    void saveDriverStatusChangedEvent_shouldPersistCorrectMetadata() {
        var event = new DriverStatusChangedEvent(
                UUID.randomUUID(),
                driverId,
                UUID.randomUUID(),
                "AVAILABLE",
                Instant.now());

        outboxEventService.saveDriverStatusChangedEvent(event);

        verifySavedEvent("DRIVER_STATUS_CHANGED", driverId, event);
    }

    @Test
    void saveDriverRideAcceptedEvent_shouldPersistCorrectMetadata() {
        var event = new DriverRideAcceptedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                driverId,
                Instant.now());

        outboxEventService.saveDriverRideAcceptedEvent(event);

        verifySavedEvent("DRIVER_RIDE_ACCEPTED", driverId, event);
    }

    @Test
    void saveDriverRideRejectedEvent_shouldPersistCorrectMetadata() {
        var event = new DriverRideRejectedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                driverId,
                Instant.now());

        outboxEventService.saveDriverRideRejectedEvent(event);

        verifySavedEvent("DRIVER_RIDE_REJECTED", driverId, event);
    }

    @Test
    void saveDriverRideExpiredEvent_shouldPersistCorrectMetadata() {
        var event = new DriverRideExpiredEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                driverId,
                Instant.now());

        outboxEventService.saveDriverRideExpiredEvent(event);

        verifySavedEvent("DRIVER_RIDE_EXPIRED", driverId, event);
    }

    @Test
    void saveDriverArrivedEvent_shouldPersistCorrectMetadata() {
        var event = new DriverArrivedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                driverId,
                Instant.now());

        outboxEventService.saveDriverArrivedEvent(event);

        verifySavedEvent("DRIVER_ARRIVED", driverId, event);
    }

    @Test
    void saveDriverTripStartedEvent_shouldPersistCorrectMetadata() {
        var event = new DriverTripStartedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                driverId,
                Instant.now());

        outboxEventService.saveDriverTripStartedEvent(event);

        verifySavedEvent("DRIVER_TRIP_STARTED", driverId, event);
    }

    @Test
    void saveDriverTripCompletedEvent_shouldPersistCorrectMetadata() {
        var event = new DriverTripCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                driverId,
                Instant.now());

        outboxEventService.saveDriverTripCompletedEvent(event);

        verifySavedEvent("DRIVER_TRIP_COMPLETED", driverId, event);
    }

    private void verifySavedEvent(
            String expectedType,
            UUID expectedDriverId,
            Object originalEvent) {
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

        verify(serializer).serialize(originalEvent);
        verify(repository).save(captor.capture());

        OutboxEvent saved = captor.getValue();

        assertEquals("DRIVER", saved.getAggregateType());
        assertEquals(expectedDriverId, saved.getAggregateId());
        assertEquals(expectedType, saved.getEventType());
        assertEquals("{\"event\":\"test\"}", saved.getPayload());
    }
}
