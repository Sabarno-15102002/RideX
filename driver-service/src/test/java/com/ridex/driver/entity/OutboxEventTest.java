package com.ridex.driver.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventTest {

    @Test
    void onCreate_whenCreatedAtIsNull_shouldSetCurrentTime() {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("DRIVER")
                .aggregateId(UUID.randomUUID())
                .eventType("DRIVER_STATUS_CHANGED")
                .payload("{}")
                .build();

        event.onCreate();

        assertNotNull(event.getCreatedAt());
    }

    @Test
    void onCreate_whenCreatedAtAlreadyExists_shouldPreserveIt() {
        Instant existing = Instant.parse("2025-01-01T00:00:00Z");

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("DRIVER")
                .aggregateId(UUID.randomUUID())
                .eventType("DRIVER_STATUS_CHANGED")
                .payload("{}")
                .createdAt(existing)
                .build();

        event.onCreate();

        assertEquals(existing, event.getCreatedAt());
    }
}
