package com.ridex.driver.service;

import java.util.UUID;

import com.ridex.driver.dto.response.DriverResponse;
import com.ridex.driver.dto.response.VehicleResponse;
import com.ridex.driver.utilities.DriverStatus;


public interface DriverService {

    DriverResponse getDriver(UUID userId);

    DriverResponse updateStatus(UUID driverId, DriverStatus requestedStatus);

    VehicleResponse getVehicle(UUID driverId);

}
