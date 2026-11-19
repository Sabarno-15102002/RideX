package com.ridex.pricing.exception;

import com.ridex.pricing.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);

        when(request.getRequestURI())
                .thenReturn("/api/v1/pricing/trips/test");

        when(request.getHeader("X-Correlation-Id"))
                .thenReturn("correlation-123");
    }

    @Test
    void shouldHandleResourceNotFound() {
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(
                new ResourceNotFoundException("Quote not found"),
                request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().code());
        assertEquals("Quote not found", response.getBody().message());
        assertEquals(
                "/api/v1/pricing/trips/test",
                response.getBody().path());
        assertEquals("correlation-123", response.getBody().correlationId());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void shouldHandleInvalidCredentials() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(
                new InvalidCredentialsException("Authentication required"),
                request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().status());
        assertEquals("INVALID_CREDENTIALS", response.getBody().code());
        assertEquals("Authentication required", response.getBody().message());
        assertEquals("correlation-123", response.getBody().correlationId());
    }

    @Test
    void shouldHandleGenericException() {
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(
                new RuntimeException("Unexpected failure"),
                request);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertEquals(
                "INTERNAL_SERVER_ERROR",
                response.getBody().code());
        assertEquals("Unexpected failure", response.getBody().message());
        assertEquals("correlation-123", response.getBody().correlationId());
    }

    @Test
    void shouldAllowMissingCorrelationId() {
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(
                new RuntimeException("Failure"),
                request);

        assertNotNull(response.getBody());
        assertNull(response.getBody().correlationId());
    }
}
