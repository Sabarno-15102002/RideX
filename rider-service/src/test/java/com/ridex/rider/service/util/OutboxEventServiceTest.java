package com.ridex.rider.service.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;

import com.ridex.rider.entity.OutboxEvent;
import com.ridex.rider.event.EventSerializer;
import com.ridex.rider.event.RiderCreatedEvent;
import com.ridex.rider.repository.OutboxEventRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private EventSerializer eventSerializer;

    @InjectMocks
    private OutboxEventService outboxEventService;

    @Test
    void saveRiderCreatedEvent_shouldSerializeAndPersistOutboxEvent() {
        UUID riderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RiderCreatedEvent event = new RiderCreatedEvent(
                UUID.randomUUID(),
                riderId,
                userId,
                Instant.now());

        String serializedPayload = "{\"eventType\":\"RIDER_CREATED\"}";

        when(eventSerializer.serialize(event))
                .thenReturn(serializedPayload);

        outboxEventService.saveRiderCreatedEvent(event);

        verify(eventSerializer).serialize(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent savedEvent = captor.getValue();

        assertEquals("RIDER", savedEvent.getAggregateType());
        assertEquals(riderId, savedEvent.getAggregateId());
        assertEquals("RIDER_CREATED", savedEvent.getEventType());
        assertEquals(serializedPayload, savedEvent.getPayload());
        assertNull(savedEvent.getPublishedAt());
    }

    @Test
    void saveRiderCreatedEvent_shouldNotPersistWhenSerializationFails() {
        UUID riderId = UUID.randomUUID();

        RiderCreatedEvent event = new RiderCreatedEvent(
                UUID.randomUUID(),
                riderId,
                UUID.randomUUID(),
                Instant.now());

        when(eventSerializer.serialize(event))
                .thenThrow(new RuntimeException("Serialization failed"));

        assertThrows(
                RuntimeException.class,
                () -> outboxEventService.saveRiderCreatedEvent(event));

        verify(outboxEventRepository, never()).save(any());
    }
}
