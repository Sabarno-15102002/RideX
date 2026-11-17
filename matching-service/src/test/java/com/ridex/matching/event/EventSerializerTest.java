
package com.ridex.matching.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridex.matching.exception.EventSerializationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventSerializerTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventSerializer eventSerializer;

    @Test
    void serialize_shouldReturnSerializedEvent() throws Exception {
        DriverMatchRequestedEvent event = new DriverMatchRequestedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now());

        String expectedJson = """
                {"eventId":"test","tripId":"test","driverId":"test"}
                """;

        when(objectMapper.writeValueAsString(event))
                .thenReturn(expectedJson);

        String result = eventSerializer.serialize(event);

        assertThat(result).isEqualTo(expectedJson);
        verify(objectMapper).writeValueAsString(event);
    }

    @Test
    void serialize_shouldWrapJsonProcessingException() throws Exception {
        Object event = new Object();

        JsonProcessingException cause = new JsonProcessingException("Serialization failed") {
        };

        when(objectMapper.writeValueAsString(event))
                .thenThrow(cause);

        EventSerializationException exception = assertThrows(
                EventSerializationException.class,
                () -> eventSerializer.serialize(event));

        assertThat(exception)
                .hasMessage("Failed to serialize event");

        assertThat(exception.getCause()).isSameAs(cause);
    }
}
