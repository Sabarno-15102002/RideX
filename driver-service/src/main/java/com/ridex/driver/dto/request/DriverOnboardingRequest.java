package com.ridex.driver.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DriverOnboardingRequest(

        @NotBlank
        @Size(max = 50)
        String licenseNumber,

        @Valid
        VehicleOnboardingRequest vehicle
) {
}