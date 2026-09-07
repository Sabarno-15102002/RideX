package com.ridex.driver.dto.response;

import java.util.UUID;

import com.ridex.driver.utilities.VehicleType;

public record VehicleResponse(
        UUID id,
        UUID driverId,
        String registrationNumber,
        String make,
        String model,
        String color,
        VehicleType vehicleType
) {
}