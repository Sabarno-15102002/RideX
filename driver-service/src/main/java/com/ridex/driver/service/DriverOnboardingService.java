package com.ridex.driver.service;

import java.util.UUID;

import com.ridex.driver.dto.request.DriverOnboardingRequest;


public interface DriverOnboardingService {

    void onboard(UUID driverId, DriverOnboardingRequest request);

}
