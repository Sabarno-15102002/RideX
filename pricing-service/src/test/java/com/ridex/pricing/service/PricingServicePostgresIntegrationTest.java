package com.ridex.pricing.service;

import com.ridex.pricing.config.PricingProperties;
import com.ridex.pricing.entity.FareQuote;
import com.ridex.pricing.entity.RiderIdentity;
import com.ridex.pricing.event.RiderCreatedEvent;
import com.ridex.pricing.event.TripCompletedEvent;
import com.ridex.pricing.repository.FareQuoteRepository;
import com.ridex.pricing.repository.ProcessedEventRepository;
import com.ridex.pricing.repository.RiderIdentityRepository;
import com.ridex.pricing.service.Impl.FareCalculationServiceImpl;
import com.ridex.pricing.service.Impl.RiderIdentityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        FareCalculationServiceImpl.class,
        RiderIdentityServiceImpl.class,
        PricingProperties.class
})
@Testcontainers
class PricingServicePostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private FareCalculationService fareCalculationService;

    @Autowired
    private RiderIdentityService riderIdentityService;

    @Autowired
    private FareQuoteRepository fareQuoteRepository;

    @Autowired
    private RiderIdentityRepository riderIdentityRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanDatabase() {
        processedEventRepository.deleteAllInBatch();
        fareQuoteRepository.deleteAllInBatch();
        riderIdentityRepository.deleteAllInBatch();
    }

    @Test
    void shouldCalculateAndPersistFareQuote() {
        UUID tripId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();

        TripCompletedEvent event = new TripCompletedEvent(
                UUID.randomUUID(),
                tripId,
                riderId,
                UUID.randomUUID(),
                0,
                0,
                0,
                1,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T11:00:00Z"));

        FareQuote quote = fareCalculationService.createFareQuote(event);

        assertNotNull(quote.getId());
        assertEquals(tripId, quote.getTripId());
        assertEquals(riderId, quote.getRiderId());

        FareQuote persisted = fareQuoteRepository.findByTripId(tripId)
                .orElseThrow();

        assertEquals(quote.getTotalFare(), persisted.getTotalFare());
        assertEquals("INR", persisted.getCurrency());
    }

    @Test
    void shouldNotCreateDuplicateFareQuoteForSameTrip() {
        UUID tripId = UUID.randomUUID();

        TripCompletedEvent firstEvent = new TripCompletedEvent(
                UUID.randomUUID(),
                tripId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                0,
                0,
                0,
                1,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T11:00:00Z"));

        FareQuote first = fareCalculationService.createFareQuote(firstEvent);

        TripCompletedEvent secondEvent = new TripCompletedEvent(
                UUID.randomUUID(),
                tripId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                10,
                10,
                11,
                11,
                Instant.parse("2026-01-01T12:00:00Z"),
                Instant.parse("2026-01-01T13:00:00Z"));

        FareQuote second = fareCalculationService.createFareQuote(secondEvent);

        assertEquals(first.getId(), second.getId());
        assertEquals(1, fareQuoteRepository.count());
    }

    @Test
    void shouldRetrieveFareQuoteForMatchingRider() {
        UUID tripId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();

        TripCompletedEvent event = new TripCompletedEvent(
                UUID.randomUUID(),
                tripId,
                riderId,
                UUID.randomUUID(),
                12,
                77,
                12.1,
                77.1,
                Instant.now(),
                Instant.now().plusSeconds(600));

        fareCalculationService.createFareQuote(event);

        var response = fareCalculationService.getFareQuote(tripId, riderId);

        assertEquals(tripId, response.tripId());
        assertEquals(riderId, response.riderId());
        assertEquals("INR", response.currency());
    }

    @Test
    void shouldCreateRiderIdentityAndProcessedEvent() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();

        riderIdentityService.handleRiderCreated(
                new RiderCreatedEvent(
                        eventId,
                        riderId,
                        userId,
                        Instant.now()));

        assertTrue(riderIdentityRepository.existsById(userId));
        assertTrue(processedEventRepository.existsById(eventId));

        assertEquals(
                riderId,
                riderIdentityService.getRiderIdByUserId(userId));
    }

    @Test
    void shouldIgnoreDuplicateRiderCreatedEvent() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();

        RiderCreatedEvent event = new RiderCreatedEvent(
                eventId,
                riderId,
                userId,
                Instant.now());

        riderIdentityService.handleRiderCreated(event);
        riderIdentityService.handleRiderCreated(event);

        assertEquals(1, riderIdentityRepository.count());
        assertEquals(1, processedEventRepository.count());
    }

    @Test
    void shouldMarkNewEventProcessedWhenIdentityAlreadyExists() {
        UUID userId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();

        riderIdentityRepository.saveAndFlush(
                RiderIdentity.builder()
                        .userId(userId)
                        .riderId(riderId)
                        .build());

        UUID eventId = UUID.randomUUID();

        riderIdentityService.handleRiderCreated(
                new RiderCreatedEvent(
                        eventId,
                        riderId,
                        userId,
                        Instant.now()));

        assertEquals(1, riderIdentityRepository.count());
        assertTrue(processedEventRepository.existsById(eventId));
    }
}
