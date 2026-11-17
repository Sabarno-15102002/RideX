package com.ridex.rider.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.ridex.rider.entity.OutboxEvent;
import com.ridex.rider.entity.ProcessedEvent;
import com.ridex.rider.entity.Rider;
import com.ridex.rider.entity.SavedLocation;
import com.ridex.rider.repository.OutboxEventRepository;
import com.ridex.rider.repository.ProcessedEventRepository;
import com.ridex.rider.repository.RiderRepository;
import com.ridex.rider.repository.SavedLocationRepository;

class RiderPersistenceIntegrationTest extends PostgresIntegrationTestBase {

        @Autowired
        private RiderRepository riderRepository;

        @Autowired
        private SavedLocationRepository savedLocationRepository;

        @Autowired
        private ProcessedEventRepository processedEventRepository;

        @Autowired
        private OutboxEventRepository outboxEventRepository;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private Flyway flyway;

        // ---------------------------------------------------------
        // Rider repository
        // ---------------------------------------------------------

        @Test
        void riderRepository_shouldSaveRiderAndGenerateIdAndTimestamps() {
                UUID userId = UUID.randomUUID();

                Rider rider = Rider.builder()
                                .userId(userId)
                                .name("John")
                                .build();

                Rider saved = riderRepository.saveAndFlush(rider);

                assertNotNull(saved.getId());
                assertEquals(userId, saved.getUserId());
                assertEquals("John", saved.getName());
                assertEquals(new BigDecimal("5.0"), saved.getRating());
                assertNotNull(saved.getCreatedAt());
                assertNotNull(saved.getUpdatedAt());
        }

        @Test
        void riderRepository_shouldFindRiderByUserId() {
                UUID userId = UUID.randomUUID();

                Rider rider = saveRider(userId, "John");

                var result = riderRepository.findByUserId(userId);

                assertTrue(result.isPresent());
                assertEquals(rider.getId(), result.get().getId());
        }

        @Test
        void riderRepository_shouldCheckWhetherUserExists() {
                UUID userId = UUID.randomUUID();

                assertFalse(riderRepository.existsByUserId(userId));

                saveRider(userId, "John");

                assertTrue(riderRepository.existsByUserId(userId));
        }

        @Test
        void riderRepository_shouldRejectDuplicateUserId() {
                UUID userId = UUID.randomUUID();

                saveRider(userId, "John");

                Rider duplicate = Rider.builder()
                                .userId(userId)
                                .name("Jane")
                                .build();

                assertThrows(
                                DataIntegrityViolationException.class,
                                () -> riderRepository.saveAndFlush(duplicate));
        }

        // ---------------------------------------------------------
        // Saved location repository
        // ---------------------------------------------------------

        @Test
        void savedLocationRepository_shouldSaveLocation() {
                Rider rider = saveRider(UUID.randomUUID(), "John");

                SavedLocation location = saveLocation(rider.getId(), "Home");

                assertNotNull(location.getId());
                assertNotNull(location.getCreatedAt());
                assertEquals(rider.getId(), location.getRiderId());
        }

        @Test
        void savedLocationRepository_shouldFindAllLocationsByRiderId() {
                Rider firstRider = saveRider(UUID.randomUUID(), "John");
                Rider secondRider = saveRider(UUID.randomUUID(), "Jane");

                saveLocation(firstRider.getId(), "Home");
                saveLocation(firstRider.getId(), "Office");
                saveLocation(secondRider.getId(), "Gym");

                var locations = savedLocationRepository.findAllByRiderId(firstRider.getId());

                assertEquals(2, locations.size());
                assertTrue(locations.stream()
                                .allMatch(location -> location.getRiderId().equals(firstRider.getId())));
        }

        @Test
        void savedLocationRepository_shouldFindLocationByIdAndRiderId() {
                Rider rider = saveRider(UUID.randomUUID(), "John");
                SavedLocation location = saveLocation(rider.getId(), "Home");

                var result = savedLocationRepository.findByIdAndRiderId(
                                location.getId(),
                                rider.getId());

                assertTrue(result.isPresent());
                assertEquals(location.getId(), result.get().getId());
        }

        @Test
        void savedLocationRepository_shouldNotFindLocationForDifferentRider() {
                Rider owner = saveRider(UUID.randomUUID(), "John");
                Rider anotherRider = saveRider(UUID.randomUUID(), "Jane");

                SavedLocation location = saveLocation(owner.getId(), "Home");

                var result = savedLocationRepository.findByIdAndRiderId(
                                location.getId(),
                                anotherRider.getId());

                assertTrue(result.isEmpty());
        }

        @Test
        void savedLocationRepository_shouldRejectUnknownRiderId() {
                SavedLocation location = SavedLocation.builder()
                                .riderId(UUID.randomUUID())
                                .label("Home")
                                .latitude(12.9716)
                                .longitude(77.5946)
                                .build();

                assertThrows(
                                DataIntegrityViolationException.class,
                                () -> savedLocationRepository.saveAndFlush(location));
        }

        // ---------------------------------------------------------
        // Processed event repository
        // ---------------------------------------------------------

        @Test
        void processedEventRepository_shouldPersistEventAndTimestamp() {
                UUID eventId = UUID.randomUUID();

                ProcessedEvent event = ProcessedEvent.builder()
                                .eventId(eventId)
                                .build();

                ProcessedEvent saved = processedEventRepository.saveAndFlush(event);

                assertEquals(eventId, saved.getEventId());
                assertNotNull(saved.getProcessedAt());
                assertTrue(processedEventRepository.existsById(eventId));
        }

        @Test
        void processedEventRepository_shouldRejectDuplicateEventId() {
                UUID eventId = UUID.randomUUID();

                processedEventRepository.saveAndFlush(
                                ProcessedEvent.builder()
                                                .eventId(eventId)
                                                .build());

                assertThrows(
                                DataIntegrityViolationException.class,
                                () -> processedEventRepository.saveAndFlush(
                                                ProcessedEvent.builder()
                                                                .eventId(eventId)
                                                                .build()));
        }

        // ---------------------------------------------------------
        // Outbox repository
        // ---------------------------------------------------------

        @Test
        void outboxEventRepository_shouldPersistOutboxEvent() {
                UUID aggregateId = UUID.randomUUID();

                OutboxEvent event = outboxEventRepository.saveAndFlush(
                                OutboxEvent.builder()
                                                .aggregateType("RIDER")
                                                .aggregateId(aggregateId)
                                                .eventType("RIDER_CREATED")
                                                .payload("{\"name\":\"John\"}")
                                                .build());

                assertNotNull(event.getId());
                assertNotNull(event.getCreatedAt());
                assertNull(event.getPublishedAt());
                assertEquals(aggregateId, event.getAggregateId());
        }

        @Test
        void outboxEventRepository_shouldFindOnlyUnpublishedEventsInOrder() {
                UUID aggregateId = UUID.randomUUID();
                Instant firstTime = Instant.parse("2026-01-01T10:00:00Z");
                Instant secondTime = Instant.parse("2026-01-01T11:00:00Z");

                OutboxEvent first = saveOutboxEvent(aggregateId, firstTime);
                OutboxEvent second = saveOutboxEvent(aggregateId, secondTime);
                OutboxEvent published = saveOutboxEvent(
                                aggregateId,
                                Instant.parse("2026-01-01T12:00:00Z"));

                published.setPublishedAt(Instant.now());
                outboxEventRepository.saveAndFlush(published);

                var pending = outboxEventRepository
                                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

                assertEquals(2, pending.size());
                assertEquals(first.getId(), pending.get(0).getId());
                assertEquals(second.getId(), pending.get(1).getId());
        }

        // ---------------------------------------------------------
        // Flyway migrations
        // ---------------------------------------------------------

        @Test
        void flyway_shouldApplyAllThreeMigrations() {
                assertEquals(3, flyway.info().applied().length);

                assertTrue(tableExists("riders"));
                assertTrue(tableExists("saved_locations"));
                assertTrue(tableExists("processed_events"));
                assertTrue(tableExists("outbox_events"));
        }

        private boolean tableExists(String tableName) {
                Boolean exists = jdbcTemplate.queryForObject(
                                """
                                                SELECT EXISTS (
                                                    SELECT 1
                                                    FROM information_schema.tables
                                                    WHERE table_schema = 'public'
                                                    AND table_name = ?
                                                )
                                                """,
                                Boolean.class,
                                tableName);

                return Boolean.TRUE.equals(exists);
        }

        // ---------------------------------------------------------
        // Helpers
        // ---------------------------------------------------------

        private Rider saveRider(UUID userId, String name) {
                return riderRepository.saveAndFlush(
                                Rider.builder()
                                                .userId(userId)
                                                .name(name)
                                                .build());
        }

        private SavedLocation saveLocation(UUID riderId, String label) {
                return savedLocationRepository.saveAndFlush(
                                SavedLocation.builder()
                                                .riderId(riderId)
                                                .label(label)
                                                .latitude(12.9716)
                                                .longitude(77.5946)
                                                .address("Bengaluru")
                                                .build());
        }

        private OutboxEvent saveOutboxEvent(
                        UUID aggregateId,
                        Instant createdAt) {
                return outboxEventRepository.saveAndFlush(
                                OutboxEvent.builder()
                                                .aggregateType("RIDER")
                                                .aggregateId(aggregateId)
                                                .eventType("RIDER_CREATED")
                                                .payload("{\"event\":\"test\"}")
                                                .createdAt(createdAt)
                                                .build());
        }

        @Test
        void saveRider_withoutRating_shouldSetDefaultRatingAndTimestamps() {
                Rider rider = Rider.builder()
                                .userId(UUID.randomUUID())
                                .name("Default Rating Rider")
                                .rating(null)
                                .build();

                Rider saved = riderRepository.saveAndFlush(rider);

                assertEquals(
                                new BigDecimal("5.0"),
                                saved.getRating());

                assertNotNull(saved.getCreatedAt());
                assertNotNull(saved.getUpdatedAt());
                assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());
        }

        @Test
        void saveRider_withExistingRating_shouldPreserveRating() {
                Rider rider = Rider.builder()
                                .userId(UUID.randomUUID())
                                .name("Rated Rider")
                                .rating(new BigDecimal("4.75"))
                                .build();

                Rider saved = riderRepository.saveAndFlush(rider);

                assertEquals(
                                new BigDecimal("4.75"),
                                saved.getRating());
        }

        @Test
        void updateRider_shouldRefreshUpdatedAt() throws InterruptedException {
                Rider rider = Rider.builder()
                                .userId(UUID.randomUUID())
                                .name("Original Name")
                                .rating(new BigDecimal("5.00"))
                                .build();

                Rider saved = riderRepository.saveAndFlush(rider);

                Instant originalUpdatedAt = saved.getUpdatedAt();

                Thread.sleep(5);

                saved.setName("Updated Name");

                Rider updated = riderRepository.saveAndFlush(saved);

                assertTrue(
                                updated.getUpdatedAt().isAfter(originalUpdatedAt));
        }

}
