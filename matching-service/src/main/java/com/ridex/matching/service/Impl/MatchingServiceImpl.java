package com.ridex.matching.service.Impl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ridex.matching.client.DriverServiceClient;
import com.ridex.matching.client.LocationServiceClient;
import com.ridex.matching.dto.response.NearbyDriverResponse;
import com.ridex.matching.entity.ProcessedEvent;
import com.ridex.matching.event.DriverMatchRequestedEvent;
import com.ridex.matching.event.TripRequestedEvent;
import com.ridex.matching.repository.ProcessedEventRepository;
import com.ridex.matching.service.MatchingService;
import com.ridex.matching.service.util.OutboxEventService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchingServiceImpl implements MatchingService {

        private static final double DEFAULT_SEARCH_RADIUS_KM = 5.0;

        private final LocationServiceClient locationServiceClient;
        private final DriverServiceClient driverServiceClient;
        private final ProcessedEventRepository processedEventRepository;
        private final OutboxEventService outboxEventService;

        @Override
        public void matchTrip(TripRequestedEvent tripEvent) {

                List<NearbyDriverResponse> drivers = locationServiceClient.findNearbyDrivers(
                                tripEvent.pickupLatitude(),
                                tripEvent.pickupLongitude(),
                                DEFAULT_SEARCH_RADIUS_KM);

                if (drivers.isEmpty()) {
                        return;
                }

                processedEventRepository.save(
                                ProcessedEvent.builder()
                                                .eventId(tripEvent.eventId())
                                                .build());

                for (NearbyDriverResponse candidate : drivers) {

                        boolean reserved = driverServiceClient.reserveDriver(
                                        candidate.driverId(),
                                        tripEvent.tripId());

                        if (!reserved) {
                                continue;
                        }

                        DriverMatchRequestedEvent matchEvent = new DriverMatchRequestedEvent(
                                        UUID.randomUUID(),
                                        tripEvent.tripId(),
                                        candidate.driverId(),
                                        Instant.now());

                        outboxEventService.saveDriverMatchRequestedEvent(matchEvent);

                        return;
                }
        }
}