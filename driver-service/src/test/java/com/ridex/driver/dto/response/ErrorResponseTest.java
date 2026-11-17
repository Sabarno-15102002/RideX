package com.ridex.driver.dto.response;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void constructor_shouldStoreAllComponents() {
        Instant timestamp = Instant.parse("2025-01-01T10:00:00Z");

        ErrorResponse response = new ErrorResponse(
                timestamp,
                404,
                "RESOURCE_NOT_FOUND",
                "Driver not found",
                "/api/v1/drivers/123",
                "correlation-123"
        );

        assertEquals(timestamp, response.timestamp());
        assertEquals(404, response.status());
        assertEquals("RESOURCE_NOT_FOUND", response.code());
        assertEquals("Driver not found", response.message());
        assertEquals("/api/v1/drivers/123", response.path());
        assertEquals("correlation-123", response.correlationId());
    }

    @Test
    void recordsWithSameComponents_shouldBeEqual() {
        Instant timestamp = Instant.parse("2025-01-01T10:00:00Z");

        ErrorResponse first = new ErrorResponse(
                timestamp, 404, "RESOURCE_NOT_FOUND",
                "Driver not found", "/drivers/123", "correlation-123"
        );

        ErrorResponse second = new ErrorResponse(
                timestamp, 404, "RESOURCE_NOT_FOUND",
                "Driver not found", "/drivers/123", "correlation-123"
        );

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
