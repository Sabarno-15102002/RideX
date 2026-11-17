
package com.ridex.matching.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.ridex.matching.entity.OutboxEvent;
import com.ridex.matching.event.DriverMatchRequestedEvent;
import com.ridex.matching.event.EventSerializer;
import com.ridex.matching.repository.OutboxEventRepository;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
                OutboxEventService.class,
                EventSerializer.class,
                OutboxEventServicePostgresIntegrationTest.TestConfig.class
})
@Testcontainers
class OutboxEventServicePostgresIntegrationTest {

        @Container
        static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
        }

        @Autowired
        private OutboxEventService outboxEventService;

        @Autowired
        private OutboxEventRepository outboxEventRepository;

        @Autowired
        private ObjectMapper objectMapper;

        @BeforeEach
        void cleanDatabase() {
                outboxEventRepository.deleteAll();
        }

        @Test
        void shouldPersistDriverMatchRequestedEvent() {
                UUID eventId = UUID.randomUUID();
                UUID tripId = UUID.randomUUID();
                UUID driverId = UUID.randomUUID();
                Instant requestedAt = Instant.parse("2026-11-17T10:00:00Z");

                DriverMatchRequestedEvent event = new DriverMatchRequestedEvent(
                                eventId,
                                tripId,
                                driverId,
                                requestedAt);

                outboxEventService.saveDriverMatchRequestedEvent(event);

                outboxEventRepository.flush();

                assertThat(outboxEventRepository.count()).isEqualTo(1);

                OutboxEvent saved = outboxEventRepository.findAll().getFirst();

                assertThat(saved.getId()).isNotNull();
                assertThat(saved.getAggregateType()).isEqualTo("DRIVER_MATCH");
                assertThat(saved.getAggregateId()).isEqualTo(driverId);
                assertThat(saved.getEventType())
                                .isEqualTo("DRIVER_MATCH_REQUESTED");
                assertThat(saved.getCreatedAt()).isNotNull();
                assertThat(saved.getPublishedAt()).isNull();
        }

        @Test
        void shouldSerializeAllEventFieldsIntoPayload() throws Exception {
                UUID eventId = UUID.randomUUID();
                UUID tripId = UUID.randomUUID();
                UUID driverId = UUID.randomUUID();
                Instant requestedAt = Instant.parse("2026-11-17T10:00:00Z");

                DriverMatchRequestedEvent event = new DriverMatchRequestedEvent(
                                eventId,
                                tripId,
                                driverId,
                                requestedAt);

                outboxEventService.saveDriverMatchRequestedEvent(event);

                outboxEventRepository.flush();

                OutboxEvent saved = outboxEventRepository.findAll().getFirst();

                JsonNode payload = objectMapper.readTree(saved.getPayload());

                assertThat(payload.get("eventId").asText())
                                .isEqualTo(eventId.toString());

                assertThat(payload.get("tripId").asText())
                                .isEqualTo(tripId.toString());

                assertThat(payload.get("driverId").asText())
                                .isEqualTo(driverId.toString());

                assertThat(payload.get("requestedAt").asText())
                                .isEqualTo(requestedAt.toString());
        }

        @Test
        void shouldPersistMultipleDriverMatchRequestedEvents() {
                DriverMatchRequestedEvent first = new DriverMatchRequestedEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                Instant.now());

                DriverMatchRequestedEvent second = new DriverMatchRequestedEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                Instant.now());

                outboxEventService.saveDriverMatchRequestedEvent(first);
                outboxEventService.saveDriverMatchRequestedEvent(second);

                outboxEventRepository.flush();

                assertThat(outboxEventRepository.count()).isEqualTo(2);

                assertThat(outboxEventRepository.findAll())
                                .extracting(OutboxEvent::getEventType)
                                .containsOnly("DRIVER_MATCH_REQUESTED");
        }

        @Test
        void shouldCreateIndependentOutboxRecordsForDifferentDrivers() {
                UUID firstDriverId = UUID.randomUUID();
                UUID secondDriverId = UUID.randomUUID();

                outboxEventService.saveDriverMatchRequestedEvent(
                                new DriverMatchRequestedEvent(
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                firstDriverId,
                                                Instant.now()));

                outboxEventService.saveDriverMatchRequestedEvent(
                                new DriverMatchRequestedEvent(
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                secondDriverId,
                                                Instant.now()));

                outboxEventRepository.flush();

                assertThat(outboxEventRepository.findAll())
                                .extracting(OutboxEvent::getAggregateId)
                                .containsExactlyInAnyOrder(
                                                firstDriverId,
                                                secondDriverId);
        }

        @TestConfiguration
        static class TestConfig {

                @Bean
                ObjectMapper objectMapper() {
                        return new ObjectMapper()
                                        .registerModule(new JavaTimeModule())
                                        .disable(
                                                        com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                }
        }
}
