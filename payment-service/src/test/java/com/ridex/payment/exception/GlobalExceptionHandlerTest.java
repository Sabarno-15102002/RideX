
package com.ridex.payment.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.ridex.payment.dto.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();

        request = mock(HttpServletRequest.class);

        when(request.getRequestURI())
                .thenReturn("/api/v1/payments/test");

        when(request.getHeader("X-Correlation-Id"))
                .thenReturn("test-correlation-id");
    }

    @Test
    void handlesInvalidPaymentStateException() {
        InvalidPaymentStateException exception = new InvalidPaymentStateException("Invalid payment state");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidPaymentState(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.NOT_FOUND,
                "INVALID_PAYMENT_STATE",
                "Invalid payment state");
    }

    @Test
    void handlesPaymentProviderException() {
        PaymentProviderException exception = new PaymentProviderException("Payment provider failed");

        ResponseEntity<ErrorResponse> response = handler.handlePaymentProviderFailure(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "BAD_PAYMENT_REQUEST",
                "Payment provider failed");
    }

    @Test
    void handlesInvalidWebhookSignatureException() {
        InvalidWebhookSignatureException exception = new InvalidWebhookSignatureException("Invalid webhook signature");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidWebhookSignature(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.UNAUTHORIZED,
                "INVALID_WEBHOOK_SIGNATURE",
                "Invalid webhook signature");
    }

    @Test
    void handlesInvalidCredentialsException() {
        InvalidCredentialsException exception = new InvalidCredentialsException("Invalid credentials");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "Invalid credentials");
    }

    @Test
    void handlesEventSerializationException() {
        EventSerializationException exception = new EventSerializationException(
                "Failed to serialize event",
                new RuntimeException("Serialization cause"));

        ResponseEntity<ErrorResponse> response = handler.handleEventSerializationException(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "EVENT_SERIALIZATION_ERROR",
                "Failed to serialize event");
    }

    @Test
    void handlesIllegalOperationException() {
        IllegalOperationException exception = new IllegalOperationException("Operation not allowed");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalOperation(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.FORBIDDEN,
                "ILLEGAL_OPERATION",
                "Operation not allowed");
    }

    @Test
    void handlesResourceNotFoundException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Payment not found");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "Payment not found");
    }

    @Test
    void handlesGenericException() {
        Exception exception = new Exception("Unexpected error");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Unexpected error");
    }

    @Test
    void handlesValidationException() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");

        bindingResult.addError(
                new FieldError(
                        "request",
                        "paymentMethod",
                        "must not be null"));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "paymentMethod: must not be null");
    }

    @Test
    void handlesValidationExceptionWithoutFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed");
    }

    @Test
    void handlesUnreadableRequestBody() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Malformed JSON",
                mock(HttpInputMessage.class));

        ResponseEntity<ErrorResponse> response = handler.handleUnreadableRequest(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST_BODY",
                "Request body is missing, malformed, or contains invalid values");
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
        assertEquals("/api/v1/payments/test", body.path());
        assertEquals("test-correlation-id", body.correlationId());
        assertNotNull(body.timestamp());
        assertNotNull(body.timestamp());
    }
}
