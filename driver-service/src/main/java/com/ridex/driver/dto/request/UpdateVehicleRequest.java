package com.ridex.driver.dto.request;

import com.ridex.driver.utilities.VehicleType;

import jakarta.validation.constraints.Size;

public record UpdateVehicleRequest(

    @Size(max = 30, message = "Registration number must not exceed 30 characters")
    String registrationNumber,

    @Size(max = 100, message = "Make must not exceed 100 characters")
    String make,

    @Size(max = 100, message = "Model must not exceed 100 characters")
    String model,

    @Size(max = 50, message = "Color must not exceed 50 characters")
    String color,
    
    VehicleType vehicleType
) {
}
