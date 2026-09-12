package com.ridex.trip.service;

import com.ridex.trip.event.RiderCreatedEvent;

public interface RiderIdentityService {

    void handleRiderCreated(RiderCreatedEvent event);

}
