package com.ridex.trip.service.util;

import com.ridex.trip.entity.OutboxEvent;
import com.ridex.trip.event.EventSerializer;
import com.ridex.trip.event.event.TripCompletedEvent;
import com.ridex.trip.event.event.TripRematchingEvent;
import com.ridex.trip.event.event.TripRequestedEvent;
import com.ridex.trip.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {
    @Mock OutboxEventRepository repository;
    @Mock EventSerializer serializer;
    @InjectMocks OutboxEventService service;

    UUID eventId, tripId, riderId, driverId;
    Instant now;

    @BeforeEach void setUp() {
        eventId = UUID.randomUUID(); tripId = UUID.randomUUID();
        riderId = UUID.randomUUID(); driverId = UUID.randomUUID();
        now = Instant.parse("2026-11-10T10:00:00Z");
    }

    @Test void saveTripRequestedEvent_shouldSerializeAndPersistOutboxRecord() {
        TripRequestedEvent event = new TripRequestedEvent(eventId, tripId, riderId,
                12.9, 77.5, 12.8, 77.6, now);
        when(serializer.serialize(event)).thenReturn("{trip-requested-json}");

        service.saveTripRequestedEvent(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertEquals(eventId, saved.getId());
        assertEquals("TRIP", saved.getAggregateType());
        assertEquals(tripId, saved.getAggregateId());
        assertEquals("TRIP_REQUESTED", saved.getEventType());
        assertEquals("{trip-requested-json}", saved.getPayload());
    }

    @Test void saveTripRematchingEvent_shouldPersistRematchingType() {
        TripRematchingEvent event = new TripRematchingEvent(eventId, tripId, riderId,
                driverId, 12.9, 77.5, 12.8, 77.6, now);
        when(serializer.serialize(event)).thenReturn("{rematching-json}");

        service.saveTripRematchingEvent(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        assertEquals("TRIP_REMATCHING", captor.getValue().getEventType());
        assertEquals("{rematching-json}", captor.getValue().getPayload());
    }

    @Test void saveTripCompletedEvent_shouldPersistCompletedType() {
        TripCompletedEvent event = new TripCompletedEvent(eventId, tripId, riderId, driverId,
                12.9, 77.5, 12.8, 77.6, now, now.plusSeconds(900));
        when(serializer.serialize(event)).thenReturn("{completed-json}");

        service.saveTripCompletedEvent(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        assertEquals("TRIP_COMPLETED", captor.getValue().getEventType());
        assertEquals("{completed-json}", captor.getValue().getPayload());
    }

    @Test void serializationFailure_shouldNotPersistOutboxRecord() {
        TripRequestedEvent event = new TripRequestedEvent(eventId, tripId, riderId,
                12.9, 77.5, 12.8, 77.6, now);
        when(serializer.serialize(event)).thenThrow(new IllegalStateException("serialization failed"));

        assertThrows(IllegalStateException.class, () -> service.saveTripRequestedEvent(event));
        verify(repository, never()).save(any(OutboxEvent.class));
    }
}
