package com.ridex.driver.dto.request;

import com.ridex.driver.utilities.DriverStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateDriverStatusRequest(

        @NotNull
        DriverStatus status
) {
}