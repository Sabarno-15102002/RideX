package com.ridex.rider.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ridex.rider.dto.response.ErrorResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();

        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/riders/me");
        request.addHeader("X-Correlation-Id", "test-correlation-id");
    }

    @Test
    void handleRiderNotFound_shouldReturnNotFound() {
        ResponseEntity<ErrorResponse> response = handler.handleRiderNotFound(
                new RiderNotFoundException("Rider not found"), request);

        assertErrorResponse(response, HttpStatus.NOT_FOUND,
                "RIDER_NOT_FOUND", "Rider not found");
    }

    @Test
    void handleLocationNotFound_shouldReturnNotFound() {
        ResponseEntity<ErrorResponse> response = handler.handleLocationNotFound(
                new SavedLocationNotFoundException("Location not found"), request);

        assertErrorResponse(response, HttpStatus.NOT_FOUND,
                "SAVED_LOCATION_NOT_FOUND", "Location not found");
    }

    @Test
    void handleEventSerializationException_shouldReturnInternalServerError() {
        ResponseEntity<ErrorResponse> response =
                handler.handleEventSerializationException(
                        new EventSerializationException("Serialization failed", new RuntimeException("Root cause")), request);

        assertErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR,
                "EVENT_SERIALIZATION_ERROR", "Serialization failed");
    }

    @Test
    void handleInvalidCredentials_shouldReturnUnauthorized() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidCredentials(
                        new InvalidCredentialsException("Invalid credentials"), request);

        assertErrorResponse(response, HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS", "Invalid credentials");
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        ResponseEntity<ErrorResponse> response =
                handler.handleGenericException(
                        new RuntimeException("Unexpected error"), request);

        assertErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR", "Unexpected error");
    }

    private void assertErrorResponse(
            ResponseEntity<ErrorResponse> response,
            HttpStatus expectedStatus,
            String expectedCode,
            String expectedMessage) {

        assertEquals(expectedStatus, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);

        assertEquals(expectedStatus.value(), body.status());
        assertEquals(expectedCode, body.code());
        assertEquals(expectedMessage, body.message());
        assertEquals("/api/v1/riders/me", body.path());
        assertEquals("test-correlation-id", body.correlationId());
        assertNotNull(body.timestamp());
    }
}