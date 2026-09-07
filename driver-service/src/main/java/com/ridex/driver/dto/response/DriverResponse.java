package com.ridex.driver.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.ridex.driver.utilities.DriverStatus;

public record DriverResponse(
        UUID id,
        UUID userId,
        String name,
        String licenseNumber,
        BigDecimal rating,
        DriverStatus status
) {
}