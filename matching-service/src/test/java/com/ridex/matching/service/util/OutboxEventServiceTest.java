package com.ridex.matching.service.util;

import com.ridex.matching.entity.OutboxEvent;
import com.ridex.matching.event.DriverMatchRequestedEvent;
import com.ridex.matching.event.EventSerializer;
import com.ridex.matching.exception.EventSerializationException;
import com.ridex.matching.repository.OutboxEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private EventSerializer eventSerializer;

    @InjectMocks
    private OutboxEventService outboxEventService;

    private DriverMatchRequestedEvent event;
    private String serializedPayload;

    @BeforeEach
    void setUp() {
        event = new DriverMatchRequestedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now());

        serializedPayload = """
                {"eventId":"test","tripId":"test","driverId":"test"}
                """;
    }

    @Test
    void saveDriverMatchRequestedEvent_shouldPersistCorrectOutboxEvent() {
        when(eventSerializer.serialize(event))
                .thenReturn(serializedPayload);

        outboxEventService.saveDriverMatchRequestedEvent(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent savedEvent = captor.getValue();

        assertThat(savedEvent.getAggregateType())
                .isEqualTo("DRIVER_MATCH");

        assertThat(savedEvent.getAggregateId())
                .isEqualTo(event.driverId());

        assertThat(savedEvent.getEventType())
                .isEqualTo("DRIVER_MATCH_REQUESTED");

        assertThat(savedEvent.getPayload())
                .isEqualTo(serializedPayload);

        assertThat(savedEvent.getId()).isNull();
        assertThat(savedEvent.getPublishedAt()).isNull();

        verify(eventSerializer).serialize(event);
    }

    @Test
    void saveDriverMatchRequestedEvent_shouldNotPersistWhenSerializationFails() {
        when(eventSerializer.serialize(event))
                .thenThrow(
                        new EventSerializationException(
                                "Failed to serialize event",
                                new RuntimeException("Serialization error")));

        assertThrows(
                EventSerializationException.class,
                () -> outboxEventService.saveDriverMatchRequestedEvent(event));

        verify(eventSerializer).serialize(event);
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void saveDriverMatchRequestedEvent_shouldSaveEachEventIndependently() {
        DriverMatchRequestedEvent secondEvent = new DriverMatchRequestedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now());

        when(eventSerializer.serialize(any()))
                .thenReturn(serializedPayload);

        outboxEventService.saveDriverMatchRequestedEvent(event);
        outboxEventService.saveDriverMatchRequestedEvent(secondEvent);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(OutboxEvent::getAggregateId)
                .containsExactly(event.driverId(), secondEvent.driverId());
    }
}
