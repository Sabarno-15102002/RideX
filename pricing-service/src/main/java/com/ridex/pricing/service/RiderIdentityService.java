package com.ridex.pricing.service;

import java.util.UUID;

import com.ridex.pricing.event.RiderCreatedEvent;

public interface RiderIdentityService {

    void handleRiderCreated(RiderCreatedEvent event);

    UUID getRiderIdByUserId(UUID userId);

}