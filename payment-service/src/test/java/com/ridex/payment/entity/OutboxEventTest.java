package com.ridex.payment.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OutboxEventTest {

    @Test
    void onCreateShouldSetCreatedAtWhenNull() {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("PAYMENT")
                .aggregateId(UUID.randomUUID())
                .eventType("PAYMENT_SUCCEEDED")
                .payload("{\"status\":\"SUCCESS\"}")
                .build();

        assertNull(event.getCreatedAt());

        event.onCreate();

        assertNotNull(event.getCreatedAt());
    }

    @Test
    void onCreateShouldPreserveExistingCreatedAt() {
        Instant existing = Instant.parse("2026-01-01T10:00:00Z");

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("PAYMENT")
                .aggregateId(UUID.randomUUID())
                .eventType("PAYMENT_CREATED")
                .payload("{}")
                .createdAt(existing)
                .build();

        event.onCreate();

        assertEquals(existing, event.getCreatedAt());
    }

    @Test
    void shouldCreateAndExposeAllFields() {
        UUID id = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        Instant publishedAt = createdAt.plusSeconds(10);

        OutboxEvent event = new OutboxEvent();

        event.setId(id);
        event.setAggregateType("PAYMENT");
        event.setAggregateId(aggregateId);
        event.setEventType("PAYMENT_SUCCEEDED");
        event.setPayload("{\"paymentId\":\"123\"}");
        event.setCreatedAt(createdAt);
        event.setPublishedAt(publishedAt);

        assertEquals(id, event.getId());
        assertEquals("PAYMENT", event.getAggregateType());
        assertEquals(aggregateId, event.getAggregateId());
        assertEquals("PAYMENT_SUCCEEDED", event.getEventType());
        assertEquals("{\"paymentId\":\"123\"}", event.getPayload());
        assertEquals(createdAt, event.getCreatedAt());
        assertEquals(publishedAt, event.getPublishedAt());
    }
}
