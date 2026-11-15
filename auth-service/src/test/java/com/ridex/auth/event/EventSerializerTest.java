package com.ridex.auth.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridex.auth.exception.EventSerializationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventSerializerTest {

        @Mock
        private ObjectMapper objectMapper;

        @InjectMocks
        private EventSerializer serializer;

        @Test
        void serialize_shouldReturnJson() throws Exception {
                UserRegisteredEvent event = new UserRegisteredEvent(
                                UUID.randomUUID(), UUID.randomUUID(),
                                "Test Rider", "RIDER", Instant.now());

                when(objectMapper.writeValueAsString(event))
                                .thenReturn("{\"name\":\"Test Rider\"}");

                String result = serializer.serialize(event);

                assertEquals("{\"name\":\"Test Rider\"}", result);
                verify(objectMapper).writeValueAsString(event);
        }

        @Test
        void serialize_shouldThrowEventSerializationException()
                        throws Exception {
                UserRegisteredEvent event = new UserRegisteredEvent(
                                UUID.randomUUID(), UUID.randomUUID(),
                                "Test Rider", "RIDER", Instant.now());

                JsonProcessingException cause = new JsonProcessingException("Serialization error") {
                };

                when(objectMapper.writeValueAsString(event))
                                .thenThrow(cause);

                EventSerializationException exception = assertThrows(EventSerializationException.class,
                                () -> serializer.serialize(event));

                assertEquals("Failed to serialize event", exception.getMessage());
                assertSame(cause, exception.getCause());
        }
}
