package com.ridex.driver.service;

import com.ridex.driver.event.UserRegisteredEvent;

public interface DriverRegistrationService {

    void handleUserRegistered(UserRegisteredEvent event);

}
