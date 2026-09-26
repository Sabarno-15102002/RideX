package com.ridex.trip.service;

import java.util.UUID;

import com.ridex.trip.event.event.RiderCreatedEvent;

public interface RiderIdentityService {

    void handleRiderCreated(RiderCreatedEvent event);

    UUID getRiderIdByUserId(UUID userId);

}
