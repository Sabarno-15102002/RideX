package com.ridex.trip.service;

import java.util.UUID;

import com.ridex.trip.dto.request.CreateTripRequest;
import com.ridex.trip.dto.response.TripResponse;
import com.ridex.trip.event.DriverMatchRequestedEvent;

public interface TripService {

    TripResponse createTrip(UUID userId, CreateTripRequest request);

    void assignDriver(DriverMatchRequestedEvent event);

}
