package com.ridex.matching.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.ridex.matching.entity.OutboxEvent;
import com.ridex.matching.entity.ProcessedEvent;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class MatchingRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    // ProcessedEvent repository tests

    @Test
    void shouldPersistAndRetrieveProcessedEvent() {
        UUID eventId = UUID.randomUUID();

        ProcessedEvent event = ProcessedEvent.builder()
                .eventId(eventId)
                .build();

        ProcessedEvent saved = processedEventRepository.saveAndFlush(event);

        assertThat(saved.getEventId()).isEqualTo(eventId);
        assertThat(saved.getProcessedAt()).isNotNull();

        assertThat(processedEventRepository.findById(eventId))
                .isPresent()
                .get()
                .extracting(ProcessedEvent::getEventId)
                .isEqualTo(eventId);
    }

    @Test
    void shouldRejectDuplicateProcessedEventId() {
        UUID eventId = UUID.randomUUID();

        processedEventRepository.saveAndFlush(
                ProcessedEvent.builder()
                        .eventId(eventId)
                        .build());

        assertThatThrownBy(() -> processedEventRepository.saveAndFlush(
                ProcessedEvent.builder()
                        .eventId(eventId)
                        .build()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldDeleteProcessedEvent() {
        UUID eventId = UUID.randomUUID();

        processedEventRepository.saveAndFlush(
                ProcessedEvent.builder()
                        .eventId(eventId)
                        .build());

        processedEventRepository.deleteById(eventId);
        processedEventRepository.flush();

        assertThat(processedEventRepository.findById(eventId)).isEmpty();
    }

    // OutboxEvent repository tests

    @Test
    void shouldPersistOutboxEventAndGenerateIdAndCreatedAt() {
        OutboxEvent event = createOutboxEvent(
                UUID.randomUUID(),
                Instant.now());

        OutboxEvent saved = outboxEventRepository.saveAndFlush(event);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getPublishedAt()).isNull();

        assertThat(outboxEventRepository.findById(saved.getId()))
                .isPresent();
    }

    @Test
    void shouldFindUnpublishedEventsOrderedByCreatedAt() {
        UUID aggregateId = UUID.randomUUID();

        OutboxEvent older = createOutboxEvent(
                aggregateId,
                Instant.parse("2026-01-01T10:00:00Z"));

        OutboxEvent newer = createOutboxEvent(
                aggregateId,
                Instant.parse("2026-01-01T11:00:00Z"));

        outboxEventRepository.saveAllAndFlush(List.of(newer, older));

        List<OutboxEvent> result = outboxEventRepository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

        assertThat(result)
                .extracting(OutboxEvent::getId)
                .containsExactly(older.getId(), newer.getId());
    }

    @Test
    void shouldExcludePublishedEventsFromUnpublishedQuery() {
        OutboxEvent unpublished = createOutboxEvent(
                UUID.randomUUID(),
                Instant.now());

        OutboxEvent published = createOutboxEvent(
                UUID.randomUUID(),
                Instant.now().plusSeconds(1));
        published.setPublishedAt(Instant.now());

        outboxEventRepository.saveAllAndFlush(
                List.of(unpublished, published));

        List<OutboxEvent> result = outboxEventRepository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

        assertThat(result)
                .extracting(OutboxEvent::getId)
                .containsExactly(unpublished.getId());
    }

    @Test
    void shouldUpdatePublishedAt() {
        OutboxEvent event = outboxEventRepository.saveAndFlush(
                createOutboxEvent(UUID.randomUUID(), Instant.now()));

        Instant publishedAt = Instant.now();

        event.setPublishedAt(publishedAt);
        outboxEventRepository.saveAndFlush(event);

        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();

        assertThat(updated.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    void shouldHaveExpectedFlywayCreatedTablesAndIndexes() {
        Integer outboxTableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'outbox_events'
                        """,
                Integer.class);

        Integer processedTableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'processed_events'
                        """,
                Integer.class);

        List<String> indexes = jdbcTemplate.queryForList(
                """
                        SELECT indexname
                        FROM pg_indexes
                        WHERE schemaname = 'public'
                        """,
                String.class);

        assertThat(outboxTableCount).isEqualTo(1);
        assertThat(processedTableCount).isEqualTo(1);

        assertThat(indexes).contains(
                "idx_outbox_events_unpublished",
                "idx_outbox_events_aggregate",
                "idx_processed_events_processed_at");
    }

    private OutboxEvent createOutboxEvent(
            UUID aggregateId,
            Instant createdAt) {
        return OutboxEvent.builder()
                .aggregateType("DRIVER_MATCH")
                .aggregateId(aggregateId)
                .eventType("DRIVER_MATCH_REQUESTED")
                .payload("{\"tripId\":\"" + UUID.randomUUID() + "\"}")
                .createdAt(createdAt)
                .build();
    }
}
