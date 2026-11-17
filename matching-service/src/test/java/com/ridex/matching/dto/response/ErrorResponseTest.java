package com.ridex.matching.dto.response;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void shouldCreateErrorResponseWithAllFields() {
        Instant timestamp = Instant.now();

        ErrorResponse response = new ErrorResponse(
                timestamp,
                500,
                "INTERNAL_SERVER_ERROR",
                "Something went wrong",
                "/api/v1/matching",
                "correlation-123");

        assertEquals(timestamp, response.timestamp());
        assertEquals(500, response.status());
        assertEquals("INTERNAL_SERVER_ERROR", response.code());
        assertEquals("Something went wrong", response.message());
        assertEquals("/api/v1/matching", response.path());
        assertEquals("correlation-123", response.correlationId());
    }

    @Test
    void shouldAllowNullOptionalFields() {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                401,
                "INVALID_CREDENTIALS",
                "Invalid credentials",
                "/api/v1/matching",
                null);

        assertNull(response.correlationId());
    }
}
