package com.ridex.pricing.repository;

import com.ridex.pricing.entity.FareQuote;
import com.ridex.pricing.entity.ProcessedEvent;
import com.ridex.pricing.entity.RiderIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PricingRepositoryIntegrationTest {

        @Container
        static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
        }

        @Autowired
        private FareQuoteRepository fareQuoteRepository;

        @Autowired
        private RiderIdentityRepository riderIdentityRepository;

        @Autowired
        private ProcessedEventRepository processedEventRepository;

        @Test
        void shouldPersistAndRetrieveFareQuote() {
                UUID tripId = UUID.randomUUID();

                FareQuote quote = createQuote(tripId, UUID.randomUUID());

                fareQuoteRepository.saveAndFlush(quote);

                FareQuote saved = fareQuoteRepository.findByTripId(tripId)
                                .orElseThrow();

                assertEquals(tripId, saved.getTripId());
                assertEquals(new BigDecimal("50.00"), saved.getBaseFare());
                assertEquals(new BigDecimal("240.00"), saved.getTotalFare());
                assertEquals("INR", saved.getCurrency());
        }

        @Test
        void shouldCheckFareQuoteExistenceByTripId() {
                UUID tripId = UUID.randomUUID();

                assertFalse(fareQuoteRepository.existsByTripId(tripId));

                fareQuoteRepository.saveAndFlush(
                                createQuote(tripId, UUID.randomUUID()));

                assertTrue(fareQuoteRepository.existsByTripId(tripId));
        }

        @Test
        void shouldEnforceUniqueTripIdForFareQuotes() {
                UUID tripId = UUID.randomUUID();

                fareQuoteRepository.saveAndFlush(
                                createQuote(tripId, UUID.randomUUID()));

                fareQuoteRepository.save(
                                createQuote(tripId, UUID.randomUUID()));

                assertThrows(
                                RuntimeException.class,
                                () -> fareQuoteRepository.flush());
        }

        @Test
        void shouldPersistRiderIdentity() {
                UUID userId = UUID.randomUUID();
                UUID riderId = UUID.randomUUID();

                riderIdentityRepository.saveAndFlush(
                                RiderIdentity.builder()
                                                .userId(userId)
                                                .riderId(riderId)
                                                .build());

                RiderIdentity saved = riderIdentityRepository.findById(userId)
                                .orElseThrow();

                assertEquals(riderId, saved.getRiderId());
                assertNotNull(saved.getCreatedAt());
        }

        @Test
        void shouldCheckRiderIdentityByRiderId() {
                UUID riderId = UUID.randomUUID();

                riderIdentityRepository.saveAndFlush(
                                RiderIdentity.builder()
                                                .userId(UUID.randomUUID())
                                                .riderId(riderId)
                                                .build());

                assertTrue(riderIdentityRepository.existsByRiderId(riderId));
                assertFalse(riderIdentityRepository.existsByRiderId(UUID.randomUUID()));
        }

        @Test
        void shouldEnforceUniqueRiderId() {
                UUID riderId = UUID.randomUUID();

                riderIdentityRepository.saveAndFlush(
                                RiderIdentity.builder()
                                                .userId(UUID.randomUUID())
                                                .riderId(riderId)
                                                .build());

                riderIdentityRepository.save(
                                RiderIdentity.builder()
                                                .userId(UUID.randomUUID())
                                                .riderId(riderId)
                                                .build());

                assertThrows(
                                RuntimeException.class,
                                () -> riderIdentityRepository.flush());
        }

        @Test
        void shouldPersistProcessedEvent() {
                UUID eventId = UUID.randomUUID();

                processedEventRepository.saveAndFlush(
                                ProcessedEvent.builder()
                                                .eventId(eventId)
                                                .build());

                ProcessedEvent saved = processedEventRepository.findById(eventId)
                                .orElseThrow();

                assertEquals(eventId, saved.getEventId());
                assertNotNull(saved.getProcessedAt());
        }

        @Test
        void shouldDeleteProcessedEvent() {
                UUID eventId = UUID.randomUUID();

                processedEventRepository.saveAndFlush(
                                ProcessedEvent.builder()
                                                .eventId(eventId)
                                                .build());

                processedEventRepository.deleteById(eventId);

                assertFalse(processedEventRepository.existsById(eventId));
        }

        private FareQuote createQuote(UUID tripId, UUID riderId) {
                FareQuote quote = new FareQuote();

                quote.setId(UUID.randomUUID());
                quote.setTripId(tripId);
                quote.setRiderId(riderId);
                quote.setBaseFare(new BigDecimal("50.00"));
                quote.setDistanceKm(new BigDecimal("10.00"));
                quote.setDurationMinutes(new BigDecimal("20.00"));
                quote.setDistanceFare(new BigDecimal("150.00"));
                quote.setDurationFare(new BigDecimal("40.00"));
                quote.setTotalFare(new BigDecimal("240.00"));
                quote.setCurrency("INR");
                quote.setCreatedAt(Instant.now());
                quote.setUpdatedAt(Instant.now());

                return quote;
        }
}
