package com.ridex.location.service;

import java.util.List;
import java.util.UUID;

import com.ridex.location.dto.response.NearbyDriverResponse;
import com.ridex.location.event.DriverStatusChangedEvent;
import com.ridex.location.event.TripCompletedEvent;

public interface DriverLocationService {

    void updateDriverLocation(UUID userId, double latitude, double longitude);

    List<NearbyDriverResponse> findNearbyDrivers(double latitude, double longitude, double radiusKm);

    void updateDriverAvailability(DriverStatusChangedEvent event);

    void updateDriverIdentity(UUID userId, UUID driverId);

    void updateDriverStatus(UUID driverId, String status);

    void handleTripCompleted(TripCompletedEvent event);

}
