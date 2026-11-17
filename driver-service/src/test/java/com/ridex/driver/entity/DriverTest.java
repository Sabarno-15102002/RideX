package com.ridex.driver.entity;

import com.ridex.driver.utilities.DriverStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DriverTest {

    @Test
    void onCreate_whenRatingAndStatusAreNull_shouldApplyDefaults() {
        Driver driver = Driver.builder()
                .userId(UUID.randomUUID())
                .name("Test Driver")
                .licenseNumber("DL12345")
                .build();

        driver.onCreate();

        assertEquals(BigDecimal.valueOf(5.00), driver.getRating());
        assertEquals(DriverStatus.OFFLINE, driver.getStatus());
        assertNotNull(driver.getCreatedAt());
        assertNotNull(driver.getUpdatedAt());
        assertEquals(driver.getCreatedAt(), driver.getUpdatedAt());
    }

    @Test
    void onCreate_whenRatingAndStatusAreProvided_shouldPreserveThem() {
        Driver driver = Driver.builder()
                .userId(UUID.randomUUID())
                .name("Test Driver")
                .licenseNumber("DL12345")
                .rating(BigDecimal.valueOf(4.75))
                .status(DriverStatus.AVAILABLE)
                .build();

        driver.onCreate();

        assertEquals(BigDecimal.valueOf(4.75), driver.getRating());
        assertEquals(DriverStatus.AVAILABLE, driver.getStatus());
        assertNotNull(driver.getCreatedAt());
        assertNotNull(driver.getUpdatedAt());
    }

    @Test
    void onUpdate_shouldRefreshUpdatedAt() {
        Driver driver = Driver.builder().build();
        Instant previous = Instant.parse("2025-01-01T00:00:00Z");
        driver.setUpdatedAt(previous);

        driver.onUpdate();

        assertTrue(driver.getUpdatedAt().isAfter(previous));
    }
}
