package com.ridex.driver.entity;

import com.ridex.driver.utilities.VehicleType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VehicleTest {

    @Test
    void onCreate_shouldSetCreatedAtAndUpdatedAt() {
        Vehicle vehicle = Vehicle.builder()
                .driverId(UUID.randomUUID())
                .registrationNumber("KA01AB1234")
                .make("Toyota")
                .model("Innova")
                .color("White")
                .vehicleType(VehicleType.SUV)
                .build();

        vehicle.onCreate();

        assertNotNull(vehicle.getCreatedAt());
        assertNotNull(vehicle.getUpdatedAt());
        assertEquals(vehicle.getCreatedAt(), vehicle.getUpdatedAt());
    }

    @Test
    void onUpdate_shouldRefreshUpdatedAt() throws InterruptedException {
        Vehicle vehicle = Vehicle.builder().build();

        Instant previous = Instant.parse("2025-01-01T00:00:00Z");
        vehicle.setUpdatedAt(previous);

        vehicle.onUpdate();

        assertNotNull(vehicle.getUpdatedAt());
        assertTrue(vehicle.getUpdatedAt().isAfter(previous));
    }
}
