package com.ridex.trip.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridex.trip.event.event.TripRequestedEvent;
import com.ridex.trip.exception.EventSerializationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventSerializerTest {
    @Test void serialize_shouldReturnJson() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        EventSerializer serializer = new EventSerializer(mapper);
        TripRequestedEvent event = new TripRequestedEvent(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 12.9, 77.5, 12.8, 77.6, Instant.parse("2026-11-10T10:00:00Z"));

        String json = serializer.serialize(event);

        assertTrue(json.contains("\"tripId\""));
        assertTrue(json.contains("\"requestedAt\""));
    }

    @Test void serialize_shouldWrapJacksonFailure() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        JsonProcessingException cause = new JsonProcessingException("bad payload") {};
        when(mapper.writeValueAsString(any())).thenThrow(cause);
        EventSerializer serializer = new EventSerializer(mapper);

        EventSerializationException thrown = assertThrows(EventSerializationException.class,
                () -> serializer.serialize(new Object()));

        assertEquals("Failed to serialize event", thrown.getMessage());
        assertSame(cause, thrown.getCause());
    }
}
