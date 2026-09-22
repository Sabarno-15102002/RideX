package com.ridex.pricing.service.Impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.pricing.config.PricingProperties;
import com.ridex.pricing.dto.response.FareQuoteResponse;
import com.ridex.pricing.entity.FareQuote;
import com.ridex.pricing.event.TripCompletedEvent;
import com.ridex.pricing.exception.ResourceNotFoundException;
import com.ridex.pricing.repository.FareQuoteRepository;
import com.ridex.pricing.service.FareCalculationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FareCalculationServiceImpl implements FareCalculationService {

        private static final double EARTH_RADIUS_KM = 6371.0;

        private final FareQuoteRepository fareQuoteRepository;
        private final PricingProperties pricingProperties;

        private double calculateDistanceKm(
                        double pickupLatitude,
                        double pickupLongitude,
                        double dropoffLatitude,
                        double dropoffLongitude) {

                double lat1 = Math.toRadians(pickupLatitude);
                double lat2 = Math.toRadians(dropoffLatitude);

                double deltaLat = Math.toRadians(
                                dropoffLatitude - pickupLatitude);

                double deltaLon = Math.toRadians(
                                dropoffLongitude - pickupLongitude);

                double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                                + Math.cos(lat1)
                                                * Math.cos(lat2)
                                                * Math.sin(deltaLon / 2)
                                                * Math.sin(deltaLon / 2);

                double c = 2 * Math.atan2(
                                Math.sqrt(a),
                                Math.sqrt(1 - a));

                return EARTH_RADIUS_KM * c;
        }

        private BigDecimal calculateDurationMinutes(
                        Instant startedAt,
                        Instant completedAt) {

                long seconds = Duration.between(
                                startedAt,
                                completedAt).getSeconds();

                return BigDecimal.valueOf(seconds)
                                .divide(
                                                BigDecimal.valueOf(60),
                                                2,
                                                RoundingMode.HALF_UP);
        }

        @Override 
        @Transactional
        public FareQuote createFareQuote(TripCompletedEvent event) {

                if (fareQuoteRepository.existsByTripId(event.tripId())) {
                        return fareQuoteRepository
                                        .findByTripId(event.tripId())
                                        .orElseThrow();
                }

                double rawDistanceKm = calculateDistanceKm(
                                event.pickupLatitude(),
                                event.pickupLongitude(),
                                event.dropoffLatitude(),
                                event.dropoffLongitude());

                BigDecimal distanceKm = BigDecimal.valueOf(rawDistanceKm)
                                .setScale(2, RoundingMode.HALF_UP);

                BigDecimal durationMinutes = calculateDurationMinutes(
                                event.startedAt(),
                                event.completedAt());

                BigDecimal distanceFare = distanceKm
                                .multiply(pricingProperties.getPerKm())
                                .setScale(2, RoundingMode.HALF_UP);

                BigDecimal durationFare = durationMinutes
                                .multiply(pricingProperties.getPerMinute())
                                .setScale(2, RoundingMode.HALF_UP);

                BigDecimal totalFare = pricingProperties.getBaseFare()
                                .add(distanceFare)
                                .add(durationFare)
                                .setScale(2, RoundingMode.HALF_UP);

                FareQuote quote = new FareQuote();

                quote.setId(UUID.randomUUID());
                quote.setTripId(event.tripId());
                quote.setRiderId(event.riderId());
                quote.setBaseFare(pricingProperties.getBaseFare());
                quote.setDistanceKm(distanceKm);
                quote.setDurationMinutes(durationMinutes);
                quote.setDistanceFare(distanceFare);
                quote.setDurationFare(durationFare);
                quote.setTotalFare(totalFare);
                quote.setCurrency("INR");

                Instant now = Instant.now();
                quote.setCreatedAt(now);
                quote.setUpdatedAt(now);

                return fareQuoteRepository.save(quote);
        }

        @Override 
        @Transactional(readOnly = true)
        public FareQuoteResponse getFareQuote(UUID tripId) {

                FareQuote quote = fareQuoteRepository.findByTripId(tripId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Fare quote not found for trip: " + tripId));

                return new FareQuoteResponse(
                                quote.getTripId(),
                                quote.getRiderId(),
                                quote.getBaseFare(),
                                quote.getDistanceKm(),
                                quote.getDurationMinutes(),
                                quote.getDistanceFare(),
                                quote.getDurationFare(),
                                quote.getTotalFare(),
                                quote.getCurrency());
        }

}
