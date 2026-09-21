package com.ridex.driver.service;

import java.util.UUID;

import com.ridex.driver.event.TripCompletedEvent;

public interface DriverTripService {

    void acceptTrip(UUID userId, UUID tripId);

    void rejectTrip(UUID userId, UUID tripId);

    void arriveAtPickup(UUID userId, UUID tripId);

    void startTrip(UUID userId, UUID tripId);

    void completeTrip(UUID userId, UUID tripId);

    void handleTripCompleted(TripCompletedEvent event);

}
