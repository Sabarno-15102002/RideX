package com.ridex.rider.service;

import com.ridex.rider.event.UserRegisteredEvent;

public interface RiderRegistrationService {

    void handleUserRegistered(UserRegisteredEvent event);

}
