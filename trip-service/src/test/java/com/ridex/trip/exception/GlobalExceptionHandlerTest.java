package com.ridex.trip.exception;

import com.ridex.trip.dto.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/trips/test");
        request.addHeader("X-Correlation-Id", "corr-test-123");
        return request;
    }

    @Test void resourceNotFound_shouldReturn404AndCorrelationId() {
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(
                new ResourceNotFoundException("Trip not found"), request());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().code());
        assertEquals("Trip not found", response.getBody().message());
        assertEquals("/api/v1/trips/test", response.getBody().path());
        assertEquals("corr-test-123", response.getBody().correlationId());
    }

    @Test void illegalOperation_shouldReturn403() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalOperation(
                new IllegalOperationException("Not allowed"), request());
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("ILLEGAL_OPERATION", response.getBody().code());
    }

    @Test void invalidCredentials_shouldReturn401() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(
                new InvalidCredentialsException("Authentication required"), request());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("INVALID_CREDENTIALS", response.getBody().code());
    }

    @Test void serializationFailure_shouldReturn500() {
        ResponseEntity<ErrorResponse> response = handler.handleEventSerializationException(
                new EventSerializationException("Could not serialize", new RuntimeException()), request());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("EVENT_SERIALIZATION_ERROR", response.getBody().code());
    }

    @Test void unexpectedFailure_shouldReturn500() {
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(
                new RuntimeException("Unexpected"), request());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().code());
    }
}
