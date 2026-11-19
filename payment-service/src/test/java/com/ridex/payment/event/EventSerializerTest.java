package com.ridex.payment.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridex.payment.exception.EventSerializationException;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventSerializerTest {

    @Test
    void serializesEventToJson() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        EventSerializer serializer = new EventSerializer(mapper);

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), new BigDecimal("99.50"), "INR", Instant.now());

        String json = serializer.serialize(event);

        assertTrue(json.contains("\"currency\":\"INR\""));
        assertTrue(json.contains("\"amount\":99.50"));
    }

    @Test
    void wrapsSerializationFailure() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        when(mapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("serialization failed") {
                });

        EventSerializer serializer = new EventSerializer(mapper);

        assertThrows(EventSerializationException.class,
                () -> serializer.serialize(new Object()));
    }
}
