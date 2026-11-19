package com.ridex.pricing.service.Impl;

import com.ridex.pricing.config.PricingProperties;
import com.ridex.pricing.dto.response.FareQuoteResponse;
import com.ridex.pricing.entity.FareQuote;
import com.ridex.pricing.event.TripCompletedEvent;
import com.ridex.pricing.exception.ResourceNotFoundException;
import com.ridex.pricing.repository.FareQuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FareCalculationServiceImplTest {

        private FareQuoteRepository fareQuoteRepository;
        private PricingProperties pricingProperties;
        private FareCalculationServiceImpl service;

        private UUID eventId;
        private UUID tripId;
        private UUID riderId;
        private UUID driverId;

        @BeforeEach
        void setUp() {
                fareQuoteRepository = mock(FareQuoteRepository.class);
                pricingProperties = new PricingProperties();

                pricingProperties.setBaseFare(new BigDecimal("50.00"));
                pricingProperties.setPerKm(new BigDecimal("15.00"));
                pricingProperties.setPerMinute(new BigDecimal("2.00"));

                service = new FareCalculationServiceImpl(
                                fareQuoteRepository,
                                pricingProperties);

                eventId = UUID.randomUUID();
                tripId = UUID.randomUUID();
                riderId = UUID.randomUUID();
                driverId = UUID.randomUUID();
        }

        private TripCompletedEvent event(
                        double pickupLat,
                        double pickupLon,
                        double dropoffLat,
                        double dropoffLon,
                        Instant startedAt,
                        Instant completedAt) {
                return new TripCompletedEvent(
                                eventId,
                                tripId,
                                riderId,
                                driverId,
                                pickupLat,
                                pickupLon,
                                dropoffLat,
                                dropoffLon,
                                startedAt,
                                completedAt);
        }

        @Test
        void shouldCalculateAndSaveFareQuote() {
                TripCompletedEvent event = event(
                                0, 0, 0, 1,
                                Instant.parse("2026-01-01T10:00:00Z"),
                                Instant.parse("2026-01-01T11:30:00Z"));

                when(fareQuoteRepository.existsByTripId(tripId))
                                .thenReturn(false);

                when(fareQuoteRepository.save(any(FareQuote.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                FareQuote result = service.createFareQuote(event);

                assertNotNull(result.getId());
                assertEquals(tripId, result.getTripId());
                assertEquals(riderId, result.getRiderId());

                assertEquals(new BigDecimal("50.00"), result.getBaseFare());
                assertEquals(new BigDecimal("111.19"), result.getDistanceKm());
                assertEquals(new BigDecimal("90.00"), result.getDurationMinutes());
                assertEquals(new BigDecimal("1667.85"), result.getDistanceFare());
                assertEquals(new BigDecimal("180.00"), result.getDurationFare());
                assertEquals(new BigDecimal("1897.85"), result.getTotalFare());

                assertEquals("INR", result.getCurrency());
                assertNotNull(result.getCreatedAt());
                assertNotNull(result.getUpdatedAt());

                verify(fareQuoteRepository).save(any(FareQuote.class));
        }

        @Test
        void shouldReturnExistingQuoteWhenTripAlreadyHasQuote() {
                FareQuote existing = new FareQuote();
                existing.setTripId(tripId);
                existing.setRiderId(riderId);

                when(fareQuoteRepository.existsByTripId(tripId))
                                .thenReturn(true);
                when(fareQuoteRepository.findByTripId(tripId))
                                .thenReturn(Optional.of(existing));

                FareQuote result = service.createFareQuote(
                                event(0, 0, 0, 1,
                                                Instant.now(),
                                                Instant.now().plusSeconds(600)));

                assertSame(existing, result);
                verify(fareQuoteRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenExistingQuoteCannotBeFound() {
                when(fareQuoteRepository.existsByTripId(tripId))
                                .thenReturn(true);
                when(fareQuoteRepository.findByTripId(tripId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                java.util.NoSuchElementException.class,
                                () -> service.createFareQuote(
                                                event(0, 0, 0, 1,
                                                                Instant.now(),
                                                                Instant.now().plusSeconds(600))));

                verify(fareQuoteRepository, never()).save(any());
        }

        @Test
        void shouldCalculateZeroDistanceFare() {
                when(fareQuoteRepository.existsByTripId(tripId))
                                .thenReturn(false);
                when(fareQuoteRepository.save(any(FareQuote.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                FareQuote result = service.createFareQuote(
                                event(12.0, 77.0, 12.0, 77.0,
                                                Instant.parse("2026-01-01T10:00:00Z"),
                                                Instant.parse("2026-01-01T10:10:00Z")));

                assertEquals(new BigDecimal("0.00"), result.getDistanceKm());
                assertEquals(new BigDecimal("0.00"), result.getDistanceFare());
                assertEquals(new BigDecimal("10.00"), result.getDurationMinutes());
                assertEquals(new BigDecimal("20.00"), result.getDurationFare());
                assertEquals(new BigDecimal("70.00"), result.getTotalFare());
        }

        @Test
        void shouldRoundDurationToTwoDecimalPlaces() {
                when(fareQuoteRepository.existsByTripId(tripId))
                                .thenReturn(false);
                when(fareQuoteRepository.save(any(FareQuote.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                FareQuote result = service.createFareQuote(
                                event(0, 0, 0, 0,
                                                Instant.parse("2026-01-01T10:00:00Z"),
                                                Instant.parse("2026-01-01T10:01:30Z")));

                assertEquals(new BigDecimal("1.50"), result.getDurationMinutes());
                assertEquals(new BigDecimal("3.00"), result.getDurationFare());
        }

        @Test
        void shouldGetFareQuoteForCorrectRider() {
                FareQuote quote = sampleQuote();

                when(fareQuoteRepository.findByTripId(tripId))
                                .thenReturn(Optional.of(quote));

                FareQuoteResponse response = service.getFareQuote(tripId, riderId);

                assertEquals(tripId, response.tripId());
                assertEquals(riderId, response.riderId());
                assertEquals(new BigDecimal("1897.85"), response.totalFare());
                assertEquals("INR", response.currency());
        }

        @Test
        void shouldNotExposeQuoteToDifferentRider() {
                FareQuote quote = sampleQuote();

                when(fareQuoteRepository.findByTripId(tripId))
                                .thenReturn(Optional.of(quote));

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> service.getFareQuote(tripId, UUID.randomUUID()));
        }

        @Test
        void shouldThrowWhenRiderScopedQuoteDoesNotExist() {
                when(fareQuoteRepository.findByTripId(tripId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> service.getFareQuote(tripId, riderId));
        }

        @Test
        void shouldGetFareQuoteWithoutRiderCheckForInternalUse() {
                FareQuote quote = sampleQuote();

                when(fareQuoteRepository.findByTripId(tripId))
                                .thenReturn(Optional.of(quote));

                FareQuoteResponse response = service.getFareQuote(tripId);

                assertEquals(tripId, response.tripId());
                assertEquals(riderId, response.riderId());
                assertEquals(new BigDecimal("1897.85"), response.totalFare());
        }

        @Test
        void shouldThrowWhenInternalQuoteDoesNotExist() {
                when(fareQuoteRepository.findByTripId(tripId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> service.getFareQuote(tripId));
        }

        private FareQuote sampleQuote() {
                FareQuote quote = new FareQuote();
                quote.setId(UUID.randomUUID());
                quote.setTripId(tripId);
                quote.setRiderId(riderId);
                quote.setBaseFare(new BigDecimal("50.00"));
                quote.setDistanceKm(new BigDecimal("111.19"));
                quote.setDurationMinutes(new BigDecimal("90.00"));
                quote.setDistanceFare(new BigDecimal("1667.85"));
                quote.setDurationFare(new BigDecimal("180.00"));
                quote.setTotalFare(new BigDecimal("1897.85"));
                quote.setCurrency("INR");
                return quote;
        }
}
