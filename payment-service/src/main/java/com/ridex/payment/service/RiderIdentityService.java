package com.ridex.payment.service;

import java.util.UUID;

import com.ridex.payment.event.RiderCreatedEvent;

public interface RiderIdentityService {

    void createIdentity(RiderCreatedEvent event);

    UUID getRiderIdByUserId(UUID userId);

}
