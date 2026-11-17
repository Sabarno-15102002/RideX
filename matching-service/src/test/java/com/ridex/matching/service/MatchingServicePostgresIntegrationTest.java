package com.ridex.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.ridex.matching.client.DriverServiceClient;
import com.ridex.matching.client.LocationServiceClient;
import com.ridex.matching.config.MatchingMetrics;
import com.ridex.matching.dto.response.NearbyDriverResponse;
import com.ridex.matching.entity.OutboxEvent;
import com.ridex.matching.event.TripRequestedEvent;
import com.ridex.matching.repository.OutboxEventRepository;
import com.ridex.matching.repository.ProcessedEventRepository;
import com.ridex.matching.service.Impl.MatchingServiceImpl;
import com.ridex.matching.service.util.OutboxEventService;
import com.ridex.matching.event.EventSerializer;
import com.ridex.matching.event.TripRematchingEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
@Import({
                MatchingServiceImpl.class,
                OutboxEventService.class,
                EventSerializer.class,
                MatchingServicePostgresIntegrationTest.TestConfig.class
})
@Testcontainers
class MatchingServicePostgresIntegrationTest {

        @Container
        static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
        }

        @Autowired
        private MatchingService matchingService;

        @Autowired
        private ProcessedEventRepository processedEventRepository;

        @Autowired
        private OutboxEventRepository outboxEventRepository;

        @MockitoBean
        private LocationServiceClient locationServiceClient;

        @MockitoBean
        private DriverServiceClient driverServiceClient;

        @MockitoBean
        private MatchingMetrics matchingMetrics;

        @BeforeEach
        void cleanDatabase() {
                outboxEventRepository.deleteAll();
                processedEventRepository.deleteAll();

                reset(locationServiceClient, driverServiceClient, matchingMetrics);
        }

        @Test
        void shouldPersistProcessedEventAndOutboxWhenDriverIsReserved() {
                UUID eventId = UUID.randomUUID();
                UUID tripId = UUID.randomUUID();
                UUID riderId = UUID.randomUUID();
                UUID driverId = UUID.randomUUID();

                TripRequestedEvent event = createTripEvent(
                                eventId, tripId, riderId);

                when(locationServiceClient.findNearbyDrivers(
                                event.pickupLatitude(),
                                event.pickupLongitude(),
                                5.0)).thenReturn(List.of(
                                                new NearbyDriverResponse(driverId, 1.2)));

                when(driverServiceClient.reserveDriver(driverId, tripId))
                                .thenReturn(true);

                matchingService.matchTrip(event);

                assertThat(processedEventRepository.findById(eventId))
                                .isPresent();

                assertThat(outboxEventRepository.findAll())
                                .hasSize(1);

                OutboxEvent outboxEvent = outboxEventRepository.findAll().getFirst();

                assertThat(outboxEvent.getAggregateType())
                                .isEqualTo("DRIVER_MATCH");

                assertThat(outboxEvent.getAggregateId())
                                .isEqualTo(driverId);

                assertThat(outboxEvent.getEventType())
                                .isEqualTo("DRIVER_MATCH_REQUESTED");

                assertThat(outboxEvent.getPayload())
                                .contains(tripId.toString())
                                .contains(driverId.toString());

                verify(driverServiceClient)
                                .reserveDriver(driverId, tripId);

                verify(matchingMetrics).recordMatchingSuccess();
                verify(matchingMetrics).recordMatchingAttempt();
                verify(matchingMetrics).recordMatchingDuration(anyLong());
        }

        @Test
        void shouldTryNextDriverWhenFirstReservationFails() {
                UUID eventId = UUID.randomUUID();
                UUID tripId = UUID.randomUUID();
                UUID riderId = UUID.randomUUID();

                UUID firstDriverId = UUID.randomUUID();
                UUID secondDriverId = UUID.randomUUID();

                TripRequestedEvent event = createTripEvent(
                                eventId, tripId, riderId);

                when(locationServiceClient.findNearbyDrivers(
                                event.pickupLatitude(),
                                event.pickupLongitude(),
                                5.0)).thenReturn(List.of(
                                                new NearbyDriverResponse(firstDriverId, 1.0),
                                                new NearbyDriverResponse(secondDriverId, 2.0)));

                when(driverServiceClient.reserveDriver(firstDriverId, tripId))
                                .thenReturn(false);

                when(driverServiceClient.reserveDriver(secondDriverId, tripId))
                                .thenReturn(true);

                matchingService.matchTrip(event);

                assertThat(processedEventRepository.findById(eventId))
                                .isPresent();

                assertThat(outboxEventRepository.findAll())
                                .hasSize(1);

                OutboxEvent saved = outboxEventRepository.findAll().getFirst();

                assertThat(saved.getAggregateId())
                                .isEqualTo(secondDriverId);

                verify(driverServiceClient)
                                .reserveDriver(firstDriverId, tripId);

                verify(driverServiceClient)
                                .reserveDriver(secondDriverId, tripId);

                verify(matchingMetrics).recordMatchingFailure();
                verify(matchingMetrics).recordMatchingSuccess();
                verify(matchingMetrics).recordMatchingAttempt();
        }

        @Test
        void shouldNotPersistAnythingWhenNoNearbyDriversAreFound() {
                UUID eventId = UUID.randomUUID();

                TripRequestedEvent event = createTripEvent(
                                eventId,
                                UUID.randomUUID(),
                                UUID.randomUUID());

                when(locationServiceClient.findNearbyDrivers(
                                event.pickupLatitude(),
                                event.pickupLongitude(),
                                5.0)).thenReturn(List.of());

                matchingService.matchTrip(event);

                assertThat(processedEventRepository.findById(eventId))
                                .isEmpty();

                assertThat(outboxEventRepository.findAll())
                                .isEmpty();

                verifyNoInteractions(driverServiceClient);
                verifyNoInteractions(matchingMetrics);
        }

        @Test
        void shouldPersistProcessedEventButNotOutboxWhenAllReservationsFail() {
                UUID eventId = UUID.randomUUID();
                UUID tripId = UUID.randomUUID();

                UUID firstDriverId = UUID.randomUUID();
                UUID secondDriverId = UUID.randomUUID();

                TripRequestedEvent event = createTripEvent(
                                eventId,
                                tripId,
                                UUID.randomUUID());

                when(locationServiceClient.findNearbyDrivers(
                                event.pickupLatitude(),
                                event.pickupLongitude(),
                                5.0)).thenReturn(List.of(
                                                new NearbyDriverResponse(firstDriverId, 1.0),
                                                new NearbyDriverResponse(secondDriverId, 2.0)));

                when(driverServiceClient.reserveDriver(firstDriverId, tripId))
                                .thenReturn(false);

                when(driverServiceClient.reserveDriver(secondDriverId, tripId))
                                .thenReturn(false);

                matchingService.matchTrip(event);

                assertThat(processedEventRepository.findById(eventId))
                                .isPresent();

                assertThat(outboxEventRepository.findAll())
                                .isEmpty();

                verify(driverServiceClient, times(2))
                                .reserveDriver(any(UUID.class), eq(tripId));

                verify(matchingMetrics, times(2))
                                .recordMatchingFailure();

                verify(matchingMetrics, never())
                                .recordMatchingAttempt();
        }

        @Test
        void shouldNotPersistEventsWhenLocationServiceReturnsNoCandidates() {
                UUID eventId = UUID.randomUUID();

                TripRequestedEvent event = createTripEvent(
                                eventId,
                                UUID.randomUUID(),
                                UUID.randomUUID());

                when(locationServiceClient.findNearbyDrivers(
                                event.pickupLatitude(),
                                event.pickupLongitude(),
                                5.0)).thenReturn(List.of());

                matchingService.matchTrip(event);

                assertThat(processedEventRepository.count()).isZero();
                assertThat(outboxEventRepository.count()).isZero();

                verifyNoInteractions(driverServiceClient);
        }

        @Test
        void shouldPersistOutboxEventWhenRematchingFindsAvailableDriver() {
                UUID tripId = UUID.randomUUID();
                UUID driverId = UUID.randomUUID();

                TripRematchingEvent event = createRematchingEvent(
                                UUID.randomUUID(),
                                tripId,
                                UUID.randomUUID(),
                                UUID.randomUUID());

                when(locationServiceClient.findNearbyDrivers(
                                event.pickupLatitude(),
                                event.pickupLongitude(),
                                5.0)).thenReturn(List.of(
                                                new NearbyDriverResponse(driverId, 1.2)));

                when(driverServiceClient.reserveDriver(driverId, tripId))
                                .thenReturn(true);

                matchingService.rematch(event);

                assertThat(outboxEventRepository.findAll())
                                .hasSize(1);

                OutboxEvent saved = outboxEventRepository.findAll().getFirst();

                assertThat(saved.getAggregateType()).isEqualTo("DRIVER_MATCH");
                assertThat(saved.getAggregateId()).isEqualTo(driverId);
                assertThat(saved.getEventType()).isEqualTo("DRIVER_MATCH_REQUESTED");
                assertThat(saved.getPayload()).contains(tripId.toString());

                verify(matchingMetrics).recordMatchingAttempt();
        }

        @Test
        void shouldTryNextDriverWhenRematchingReservationFails() {
                UUID tripId = UUID.randomUUID();
                UUID firstDriver = UUID.randomUUID();
                UUID secondDriver = UUID.randomUUID();

                TripRematchingEvent event = createRematchingEvent(
                                UUID.randomUUID(),
                                tripId,
                                UUID.randomUUID(),
                                UUID.randomUUID());

                when(locationServiceClient.findNearbyDrivers(
                                event.pickupLatitude(),
                                event.pickupLongitude(),
                                5.0)).thenReturn(List.of(
                                                new NearbyDriverResponse(firstDriver, 1.0),
                                                new NearbyDriverResponse(secondDriver, 2.0)));

                when(driverServiceClient.reserveDriver(firstDriver, tripId))
                                .thenReturn(false);

                when(driverServiceClient.reserveDriver(secondDriver, tripId))
                                .thenReturn(true);

                matchingService.rematch(event);

                assertThat(outboxEventRepository.findAll())
                                .hasSize(1);

                assertThat(outboxEventRepository.findAll().getFirst().getAggregateId())
                                .isEqualTo(secondDriver);

                verify(driverServiceClient).reserveDriver(firstDriver, tripId);
                verify(driverServiceClient).reserveDriver(secondDriver, tripId);
                verify(matchingMetrics).recordMatchingAttempt();
        }

        @Test
        void shouldNotPersistOutboxEventWhenNoDriversAreFoundForRematching() {
                TripRematchingEvent event = createRematchingEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID());

                when(locationServiceClient.findNearbyDrivers(
                                event.pickupLatitude(),
                                event.pickupLongitude(),
                                5.0)).thenReturn(List.of());

                matchingService.rematch(event);

                assertThat(outboxEventRepository.findAll()).isEmpty();

                verifyNoInteractions(driverServiceClient);
                verifyNoInteractions(matchingMetrics);
        }

        @Test
        void shouldNotPersistOutboxEventWhenAllRematchingReservationsFail() {
                UUID tripId = UUID.randomUUID();
                UUID firstDriver = UUID.randomUUID();
                UUID secondDriver = UUID.randomUUID();

                TripRematchingEvent event = createRematchingEvent(
                                UUID.randomUUID(),
                                tripId,
                                UUID.randomUUID(),
                                UUID.randomUUID());

                when(locationServiceClient.findNearbyDrivers(
                                event.pickupLatitude(),
                                event.pickupLongitude(),
                                5.0)).thenReturn(List.of(
                                                new NearbyDriverResponse(firstDriver, 1.0),
                                                new NearbyDriverResponse(secondDriver, 2.0)));

                when(driverServiceClient.reserveDriver(firstDriver, tripId))
                                .thenReturn(false);

                when(driverServiceClient.reserveDriver(secondDriver, tripId))
                                .thenReturn(false);

                matchingService.rematch(event);

                assertThat(outboxEventRepository.findAll()).isEmpty();

                verify(driverServiceClient, times(2))
                                .reserveDriver(any(UUID.class), eq(tripId));

                verify(matchingMetrics, never()).recordMatchingAttempt();
        }

        private TripRematchingEvent createRematchingEvent(
                        UUID eventId,
                        UUID tripId,
                        UUID riderId,
                        UUID rejectedDriverId) {
                return new TripRematchingEvent(
                                eventId,
                                tripId,
                                riderId,
                                rejectedDriverId,
                                12.9716,
                                77.5946,
                                12.9352,
                                77.6245,
                                Instant.now());
        }

        private TripRequestedEvent createTripEvent(
                        UUID eventId,
                        UUID tripId,
                        UUID riderId) {
                return new TripRequestedEvent(
                                eventId,
                                tripId,
                                riderId,
                                12.9716,
                                77.5946,
                                12.9352,
                                77.6245,
                                Instant.now());
        }

        @TestConfiguration
        static class TestConfig {

                @Bean
                ObjectMapper objectMapper() {
                        return new ObjectMapper()
                                        .registerModule(new JavaTimeModule());
                }
        }
}
