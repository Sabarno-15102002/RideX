package com.ridex.driver.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcessedEventTest {

    @Test
    void onCreate_whenProcessedAtIsNull_shouldSetCurrentTime() {
        ProcessedEvent event = ProcessedEvent.builder()
                .eventId(UUID.randomUUID())
                .build();

        event.onCreate();

        assertNotNull(event.getProcessedAt());
    }

    @Test
    void onCreate_whenProcessedAtAlreadyExists_shouldPreserveIt() {
        Instant existing = Instant.parse("2025-01-01T00:00:00Z");

        ProcessedEvent event = ProcessedEvent.builder()
                .eventId(UUID.randomUUID())
                .processedAt(existing)
                .build();

        event.onCreate();

        assertEquals(existing, event.getProcessedAt());
    }
}
