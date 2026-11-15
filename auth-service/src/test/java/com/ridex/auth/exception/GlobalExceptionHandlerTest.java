package com.ridex.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private void configureRequest() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Correlation-Id"))
                .thenReturn("test-correlation-id");
    }

    @Test
    void handleResourceAlreadyExists_shouldReturnConflict() {
        configureRequest();

        var response = handler.handleResourceAlreadyExists(
                new ResourceAlreadyExistsException("Already exists"),
                request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("RESOURCE_ALREADY_EXISTS",
                response.getBody().code());
        assertEquals("/api/v1/auth/login", response.getBody().path());
        assertEquals("test-correlation-id",
                response.getBody().correlationId());
    }

    @Test
    void handleInvalidCredentials_shouldReturnUnauthorized() {
        configureRequest();

        var response = handler.handleInvalidCredentials(
                new InvalidCredentialsException("Invalid credentials"),
                request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("INVALID_CREDENTIALS", response.getBody().code());
    }

    @Test
    void handleInvalidRefreshToken_shouldReturnUnauthorized() {
        configureRequest();

        var response = handler.handleInvalidRefreshToken(
                new InvalidRefreshTokenException("Invalid token"),
                request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("INVALID_REFRESH_TOKEN", response.getBody().code());
    }

    @Test
    void handleEventSerializationException_shouldReturnInternalServerError() {
        configureRequest();

        var response = handler.handleEventSerializationException(
                new EventSerializationException(
                        "Serialization failed", new RuntimeException()),
                request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());
        assertEquals("EVENT_SERIALIZATION_ERROR",
                response.getBody().code());
    }

    @Test
    void handleInvalidAlgorithmParameterException_shouldReturnBadRequest() {
        configureRequest();

        var response = handler.handleInvalidAlgorithmParameterException(
                new InvalidAlgorithmParameterException(
                        new RuntimeException()),
                request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_ALGORITHM_PARAMETER",
                response.getBody().code());
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        configureRequest();

        var response = handler.handleGenericException(
                new RuntimeException("Unexpected error"),
                request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().code());
        assertEquals("Unexpected error", response.getBody().message());
    }
}
