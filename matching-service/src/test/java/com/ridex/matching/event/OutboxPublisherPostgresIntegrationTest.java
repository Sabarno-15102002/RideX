package com.ridex.matching.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.ridex.matching.entity.OutboxEvent;
import com.ridex.matching.repository.OutboxEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OutboxPublisher.class)
@Testcontainers
class OutboxPublisherPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean(name = "outboxKafkaTemplate")
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        reset(kafkaTemplate);
    }

    @Test
    void shouldPublishEventAndMarkItAsPublished() {
        UUID driverId = UUID.randomUUID();

        OutboxEvent event = createEvent(driverId);
        outboxEventRepository.saveAndFlush(event);

        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                anyString())).thenReturn(CompletableFuture.completedFuture(null));

        outboxPublisher.publishEvents();

        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();

        assertThat(updated.getPublishedAt()).isNotNull();

        verify(kafkaTemplate).send(
                eq("driver.match.requested"),
                eq(driverId.toString()),
                eq(event.getPayload()));
    }

    @Test
    void shouldKeepEventUnpublishedWhenKafkaSendFails() {
        OutboxEvent event = createEvent(UUID.randomUUID());
        outboxEventRepository.saveAndFlush(event);

        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                anyString())).thenReturn(
                        CompletableFuture.failedFuture(
                                new RuntimeException("Kafka unavailable")));

        outboxPublisher.publishEvents();

        OutboxEvent unchanged = outboxEventRepository.findById(event.getId()).orElseThrow();

        assertThat(unchanged.getPublishedAt()).isNull();
    }

    @Test
    void shouldKeepEventUnpublishedWhenKafkaSendThrowsSynchronously() {
        OutboxEvent event = createEvent(UUID.randomUUID());
        outboxEventRepository.saveAndFlush(event);

        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                anyString())).thenThrow(new RuntimeException("Kafka unavailable"));

        outboxPublisher.publishEvents();

        OutboxEvent unchanged = outboxEventRepository.findById(event.getId()).orElseThrow();

        assertThat(unchanged.getPublishedAt()).isNull();
    }

    @Test
    void shouldNotSendAnythingWhenThereAreNoUnpublishedEvents() {
        outboxPublisher.publishEvents();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldPublishMultipleUnpublishedEvents() {
        OutboxEvent first = createEvent(UUID.randomUUID());
        OutboxEvent second = createEvent(UUID.randomUUID());

        outboxEventRepository.saveAllAndFlush(
                java.util.List.of(first, second));

        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                anyString())).thenReturn(CompletableFuture.completedFuture(null));

        outboxPublisher.publishEvents();

        assertThat(outboxEventRepository.findAll())
                .allMatch(event -> event.getPublishedAt() != null);

        verify(kafkaTemplate, times(2)).send(
                eq("driver.match.requested"),
                anyString(),
                anyString());
    }

    private OutboxEvent createEvent(UUID driverId) {
        return OutboxEvent.builder()
                .aggregateType("DRIVER_MATCH")
                .aggregateId(driverId)
                .eventType("DRIVER_MATCH_REQUESTED")
                .payload("{\"driverId\":\"" + driverId + "\"}")
                .createdAt(Instant.now())
                .build();
    }
}
