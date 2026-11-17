package com.ridex.rider.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridex.rider.entity.OutboxEvent;
import com.ridex.rider.event.EventSerializer;
import com.ridex.rider.event.UserRegisteredEvent;
import com.ridex.rider.repository.OutboxEventRepository;
import com.ridex.rider.repository.ProcessedEventRepository;
import com.ridex.rider.repository.RiderRepository;
import com.ridex.rider.service.Impl.RiderRegistrationServiceImpl;
import com.ridex.rider.service.util.OutboxEventService;

@Import({
        RiderRegistrationServiceImpl.class,
        OutboxEventService.class,
        EventSerializer.class,
        RiderRegistrationIntegrationTest.JacksonTestConfig.class
})
class RiderRegistrationIntegrationTest extends PostgresIntegrationTestBase {

    @TestConfiguration
    static class JacksonTestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .registerModule(new JavaTimeModule());
        }
    }

    @Autowired
    private RiderRegistrationServiceImpl registrationService;

    @Autowired
    private RiderRepository riderRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void handleUserRegistered_shouldPersistRiderProcessedEventAndOutboxEvent() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId,
                userId,
                "John",
                "RIDER",
                Instant.now()
        );

        registrationService.handleUserRegistered(event);

        var rider = riderRepository.findByUserId(userId);

        assertTrue(rider.isPresent());
        assertEquals("John", rider.get().getName());
        assertEquals(
                new java.math.BigDecimal("5.0"),
                rider.get().getRating()
        );

        assertTrue(processedEventRepository.existsById(eventId));

        var outboxEvents =
                outboxEventRepository
                        .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

        assertEquals(1, outboxEvents.size());

        OutboxEvent outboxEvent = outboxEvents.get(0);

        assertEquals("RIDER", outboxEvent.getAggregateType());
        assertEquals("RIDER_CREATED", outboxEvent.getEventType());
        assertEquals(rider.get().getId(), outboxEvent.getAggregateId());
        assertNotNull(outboxEvent.getPayload());

        assertTrue(outboxEvent.getPayload().contains(userId.toString()));
        assertTrue(outboxEvent.getPayload().contains(rider.get().getId().toString()));
        assertTrue(outboxEvent.getPayload().contains("eventId"));
        assertTrue(outboxEvent.getPayload().contains("createdAt"));
    }

    @Test
    void handleUserRegistered_shouldIgnoreDuplicateEvent() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId,
                userId,
                "John",
                "RIDER",
                Instant.now()
        );

        registrationService.handleUserRegistered(event);
        registrationService.handleUserRegistered(event);

        assertEquals(1, riderRepository.count());
        assertEquals(1, processedEventRepository.count());
        assertEquals(1, outboxEventRepository.count());
    }

    @Test
    void handleUserRegistered_shouldMarkNonRiderEventAsProcessed() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId,
                userId,
                "Jane",
                "DRIVER",
                Instant.now()
        );

        registrationService.handleUserRegistered(event);

        assertFalse(riderRepository.existsByUserId(userId));
        assertTrue(processedEventRepository.existsById(eventId));
        assertEquals(0, outboxEventRepository.count());
    }

    @Test
    void handleUserRegistered_shouldNotCreateDuplicateRiderForNewEvent() {
        UUID userId = UUID.randomUUID();

        riderRepository.saveAndFlush(
                com.ridex.rider.entity.Rider.builder()
                        .userId(userId)
                        .name("Existing Rider")
                        .build()
        );

        UUID eventId = UUID.randomUUID();

        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId,
                userId,
                "New Name",
                "RIDER",
                Instant.now()
        );

        registrationService.handleUserRegistered(event);

        assertEquals(1, riderRepository.count());
        assertTrue(processedEventRepository.existsById(eventId));
        assertEquals(0, outboxEventRepository.count());

        var rider = riderRepository.findByUserId(userId);

        assertTrue(rider.isPresent());
        assertEquals("Existing Rider", rider.get().getName());
    }
}
