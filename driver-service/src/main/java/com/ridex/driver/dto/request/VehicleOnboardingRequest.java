package com.ridex.driver.dto.request;

import com.ridex.driver.utilities.VehicleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VehicleOnboardingRequest(

        @NotBlank
        @Size(max = 30)
        String registrationNumber,

        @NotBlank
        @Size(max = 100)
        String make,

        @NotBlank
        @Size(max = 100)
        String model,

        @NotBlank
        @Size(max = 50)
        String color,

        @NotNull
        VehicleType vehicleType
) {
}