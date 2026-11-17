package com.ridex.matching.exception;

import com.ridex.matching.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

        private GlobalExceptionHandler exceptionHandler;
        private HttpServletRequest request;

        @BeforeEach
        void setUp() {
                exceptionHandler = new GlobalExceptionHandler();
                request = mock(HttpServletRequest.class);

                when(request.getRequestURI())
                                .thenReturn("/api/v1/matching");

                when(request.getHeader("X-Correlation-Id"))
                                .thenReturn("correlation-123");
        }

        @Test
        void shouldHandleEventSerializationException() {
                EventSerializationException exception = new EventSerializationException(
                                "Failed to serialize event",
                                new RuntimeException("Serialization cause"));

                ResponseEntity<ErrorResponse> response = exceptionHandler.handleEventSerializationException(
                                exception,
                                request);

                assertEquals(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                response.getStatusCode());

                assertNotNull(response.getBody());
                assertEquals(500, response.getBody().status());
                assertEquals(
                                "EVENT_SERIALIZATION_ERROR",
                                response.getBody().code());
                assertEquals(
                                "Failed to serialize event",
                                response.getBody().message());
                assertEquals(
                                "/api/v1/matching",
                                response.getBody().path());
                assertEquals(
                                "correlation-123",
                                response.getBody().correlationId());
                assertNotNull(response.getBody().timestamp());
        }

        @Test
        void shouldHandleInvalidCredentialsException() {
                InvalidCredentialsException exception = new InvalidCredentialsException("Invalid credentials");

                ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidCredentials(
                                exception,
                                request);

                assertEquals(
                                HttpStatus.UNAUTHORIZED,
                                response.getStatusCode());

                assertNotNull(response.getBody());
                assertEquals(401, response.getBody().status());
                assertEquals(
                                "INVALID_CREDENTIALS",
                                response.getBody().code());
                assertEquals(
                                "Invalid credentials",
                                response.getBody().message());
                assertEquals(
                                "/api/v1/matching",
                                response.getBody().path());
                assertEquals(
                                "correlation-123",
                                response.getBody().correlationId());
                assertNotNull(response.getBody().timestamp());
        }

        @Test
        void shouldHandleGenericException() {
                Exception exception = new RuntimeException("Unexpected error");

                ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(
                                exception,
                                request);

                assertEquals(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                response.getStatusCode());

                assertNotNull(response.getBody());
                assertEquals(500, response.getBody().status());
                assertEquals(
                                "INTERNAL_SERVER_ERROR",
                                response.getBody().code());
                assertEquals(
                                "Unexpected error",
                                response.getBody().message());
                assertEquals(
                                "/api/v1/matching",
                                response.getBody().path());
                assertEquals(
                                "correlation-123",
                                response.getBody().correlationId());
                assertNotNull(response.getBody().timestamp());
        }

        @Test
        void shouldHandleMissingCorrelationId() {
                when(request.getHeader("X-Correlation-Id"))
                                .thenReturn(null);

                ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(
                                new RuntimeException("Unexpected error"),
                                request);

                assertNotNull(response.getBody());
                assertNull(response.getBody().correlationId());
        }
}
