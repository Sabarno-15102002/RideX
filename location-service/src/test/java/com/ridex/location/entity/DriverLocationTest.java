package com.ridex.location.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DriverLocationTest {

    @Test
    void createsDriverLocationWithExpectedValues() {
        UUID driverId = UUID.randomUUID();
        double latitude = 12.9716;
        double longitude = 77.5946;
        Instant updatedAt = Instant.now();

        DriverLocation location = new DriverLocation(
                driverId,
                latitude,
                longitude,
                updatedAt);

        assertEquals(driverId, location.driverId());
        assertEquals(latitude, location.latitude());
        assertEquals(longitude, location.longitude());
        assertEquals(updatedAt, location.updatedAt());
    }

    @Test
    void twoLocationsWithSameValuesAreEqual() {
        UUID driverId = UUID.randomUUID();
        Instant updatedAt = Instant.now();

        DriverLocation first = new DriverLocation(
                driverId, 12.9716, 77.5946, updatedAt);

        DriverLocation second = new DriverLocation(
                driverId, 12.9716, 77.5946, updatedAt);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void locationsWithDifferentValuesAreNotEqual() {
        DriverLocation first = new DriverLocation(
                UUID.randomUUID(), 12.9716, 77.5946, Instant.now());

        DriverLocation second = new DriverLocation(
                UUID.randomUUID(), 13.0000, 77.6000, Instant.now());

        assertNotEquals(first, second);
    }

    @Test
    void generatesStringRepresentation() {
        UUID driverId = UUID.randomUUID();
        Instant updatedAt = Instant.now();

        DriverLocation location = new DriverLocation(
                driverId, 12.9716, 77.5946, updatedAt);

        String representation = location.toString();

        assertTrue(representation.contains(driverId.toString()));
        assertTrue(representation.contains("12.9716"));
        assertTrue(representation.contains("77.5946"));
        assertTrue(representation.contains(updatedAt.toString()));
    }
}
