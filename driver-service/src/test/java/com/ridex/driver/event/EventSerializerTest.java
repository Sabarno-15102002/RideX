package com.ridex.driver.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridex.driver.exception.EventSerializationException;

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
    private EventSerializer serializer;

    @Test
    void serialize_shouldReturnJson() throws Exception {
        Object event = new Object();

        when(objectMapper.writeValueAsString(event))
                .thenReturn("{\"status\":\"AVAILABLE\"}");

        String result = serializer.serialize(event);

        assertEquals("{\"status\":\"AVAILABLE\"}", result);
    }

    @Test
    void serialize_shouldWrapJsonProcessingException() throws Exception {
        Object event = new Object();

        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("Serialization failed") {
                });

        EventSerializationException exception = assertThrows(
                EventSerializationException.class,
                () -> serializer.serialize(event));

        assertEquals("Failed to serialize event", exception.getMessage());
        assertNotNull(exception.getCause());
    }
}
