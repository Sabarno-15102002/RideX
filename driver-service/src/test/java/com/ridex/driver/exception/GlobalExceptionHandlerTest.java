package com.ridex.driver.exception;

import com.ridex.driver.dto.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

        private GlobalExceptionHandler handler;
        private MockHttpServletRequest request;

        @BeforeEach
        void setUp() {
                handler = new GlobalExceptionHandler();

                request = new MockHttpServletRequest();
                request.setRequestURI("/api/v1/drivers/123");
                request.addHeader("X-Correlation-Id", "correlation-123");
        }

        @Test
        void handleResourceAlreadyExists_shouldReturnConflict() {
                ResponseEntity<ErrorResponse> response = handler.handleResourceAlreadyExists(
                                new ResourceAlreadyExistsException("Driver already exists"),
                                request);

                assertErrorResponse(
                                response, 409, "RESOURCE_ALREADY_EXISTS",
                                "Driver already exists");
        }

        @Test
        void handleResourceNotFound_shouldReturnNotFound() {
                ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(
                                new ResourceNotFoundException("Driver not found"),
                                request);

                assertErrorResponse(
                                response, 404, "RESOURCE_NOT_FOUND",
                                "Driver not found");
        }

        @Test
        void handleEventSerializationException_shouldReturnInternalServerError() {
                ResponseEntity<ErrorResponse> response = handler.handleEventSerializationException(
                                new EventSerializationException(
                                                "Failed to serialize event",
                                                new RuntimeException("Serialization cause")),
                                request);

                assertErrorResponse(
                                response, 500, "EVENT_SERIALIZATION_ERROR",
                                "Failed to serialize event");
        }

        @Test
        void handleInvalidCredentials_shouldReturnUnauthorized() {
                ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(
                                new InvalidCredentialsException("Invalid credentials"),
                                request);

                assertErrorResponse(
                                response, 401, "INVALID_CREDENTIALS",
                                "Invalid credentials");
        }

        @Test
        void handleTripStateException_shouldReturnBadRequest() {
                ResponseEntity<ErrorResponse> response = handler.handleTripStateException(
                                new TripStateException("Driver is not on a trip"),
                                request);

                assertErrorResponse(
                                response, 400, "INVALID_STATE_IN_TRIP",
                                "Driver is not on a trip");
        }

        @Test
        void handleGenericException_shouldReturnInternalServerError() {
                ResponseEntity<ErrorResponse> response = handler.handleGenericException(
                                new RuntimeException("Unexpected failure"),
                                request);

                assertErrorResponse(
                                response, 500, "INTERNAL_SERVER_ERROR",
                                "Unexpected failure");
        }

        private void assertErrorResponse(
                        ResponseEntity<ErrorResponse> response,
                        int expectedStatus,
                        String expectedCode,
                        String expectedMessage) {
                assertEquals(expectedStatus, response.getStatusCode().value());

                ErrorResponse body = response.getBody();
                assertNotNull(body);

                assertNotNull(body.timestamp());
                assertEquals(expectedStatus, body.status());
                assertEquals(expectedCode, body.code());
                assertEquals(expectedMessage, body.message());
                assertEquals("/api/v1/drivers/123", body.path());
                assertEquals("correlation-123", body.correlationId());
        }
}
