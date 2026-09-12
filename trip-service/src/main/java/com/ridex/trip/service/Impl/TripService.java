package com.ridex.trip.service.Impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.trip.dto.TripStatus;
import com.ridex.trip.dto.request.CreateTripRequest;
import com.ridex.trip.dto.response.TripResponse;
import com.ridex.trip.entity.RiderIdentity;
import com.ridex.trip.entity.Trip;
import com.ridex.trip.event.TripRequestedEvent;
import com.ridex.trip.repository.RiderIdentityRepository;
import com.ridex.trip.repository.TripRepository;
import com.ridex.trip.service.util.OutboxEventService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final RiderIdentityRepository riderIdentityRepository;
    private final OutboxEventService outboxEventService;

    @Transactional
    public TripResponse createTrip(
            UUID userId,
            CreateTripRequest request
    ) {
        RiderIdentity riderIdentity =
                riderIdentityRepository.findById(userId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Rider profile not found"
                                ));

        Instant now = Instant.now();

        Trip trip = Trip.builder()
                .id(UUID.randomUUID())
                .riderId(riderIdentity.getRiderId())
                .pickupLatitude(request.pickupLatitude())
                .pickupLongitude(request.pickupLongitude())
                .dropoffLatitude(request.dropoffLatitude())
                .dropoffLongitude(request.dropoffLongitude())
                .status(TripStatus.REQUESTED)
                .requestedAt(now)
                .build();

        tripRepository.save(trip);

        TripRequestedEvent event = new TripRequestedEvent(
                UUID.randomUUID(),
                trip.getId(),
                trip.getRiderId(),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.dropoffLatitude(),
                request.dropoffLongitude(),
                now
        );

        outboxEventService.saveTripRequestedEvent(event);

        return toResponse(trip);
    }

    private TripResponse toResponse(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getRiderId(),
                trip.getDriverId(),
                trip.getPickupLatitude(),
                trip.getPickupLongitude(),
                trip.getDropoffLatitude(),
                trip.getDropoffLongitude(),
                trip.getStatus(),
                trip.getRequestedAt()
        );
    }
}