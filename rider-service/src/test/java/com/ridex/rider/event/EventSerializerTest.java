
package com.ridex.rider.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridex.rider.exception.EventSerializationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventSerializerTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventSerializer eventSerializer;

    @Test
    void serialize_shouldReturnJsonString() throws Exception {
        RiderCreatedEvent event = new RiderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now());

        String expectedJson = "{\"eventType\":\"RIDER_CREATED\"}";

        when(objectMapper.writeValueAsString(event))
                .thenReturn(expectedJson);

        String result = eventSerializer.serialize(event);

        assertEquals(expectedJson, result);
        verify(objectMapper).writeValueAsString(event);
    }

    @Test
    void serialize_shouldWrapJsonProcessingException() throws Exception {
        RiderCreatedEvent event = new RiderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now());

        JsonProcessingException cause =
                new JsonProcessingException("Serialization error") {};

        when(objectMapper.writeValueAsString(event))
                .thenThrow(cause);

        EventSerializationException exception = assertThrows(
                EventSerializationException.class,
                () -> eventSerializer.serialize(event));

        assertEquals("Failed to serialize event", exception.getMessage());
        assertSame(cause, exception.getCause());

        verify(objectMapper).writeValueAsString(event);
    }
}
