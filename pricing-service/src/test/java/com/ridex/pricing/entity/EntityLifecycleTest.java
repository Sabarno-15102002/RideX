package com.ridex.pricing.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityLifecycleTest {

        @Test
        void processedEventShouldPreserveExistingProcessedAt() {
                Instant existingTimestamp = Instant.parse("2026-01-01T10:00:00Z");

                ProcessedEvent event = ProcessedEvent.builder()
                                .eventId(UUID.randomUUID())
                                .processedAt(existingTimestamp)
                                .build();

                event.onCreate();

                assertEquals(existingTimestamp, event.getProcessedAt());
        }

        @Test
        void riderIdentityShouldPreserveExistingCreatedAt() {
                Instant existingTimestamp = Instant.parse("2026-01-01T10:00:00Z");

                RiderIdentity identity = RiderIdentity.builder()
                                .userId(UUID.randomUUID())
                                .riderId(UUID.randomUUID())
                                .createdAt(existingTimestamp)
                                .build();

                identity.onCreate();

                assertEquals(existingTimestamp, identity.getCreatedAt());
        }
}
