package com.ridex.trip.service;

import java.util.UUID;

import com.ridex.trip.dto.request.CreateTripRequest;
import com.ridex.trip.dto.response.TripResponse;
import com.ridex.trip.event.event.DriverMatchRequestedEvent;
import com.ridex.trip.event.event.DriverRideAcceptedEvent;
import com.ridex.trip.event.event.DriverRideExpiredEvent;
import com.ridex.trip.event.event.DriverRideRejectedEvent;

public interface TripService {

    TripResponse createTrip(UUID userId, CreateTripRequest request);

    void assignDriver(DriverMatchRequestedEvent event);

    void handleDriverRideAccepted(DriverRideAcceptedEvent event);

    void handleDriverRideRejected(DriverRideRejectedEvent event);

    void handleDriverRideExpired(DriverRideExpiredEvent event);

}
