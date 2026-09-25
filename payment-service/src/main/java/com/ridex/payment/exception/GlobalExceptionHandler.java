package com.ridex.payment.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ridex.payment.dto.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(InvalidPaymentStateException.class)
        public ResponseEntity<ErrorResponse> handleInvalidPaymentState(
                        InvalidPaymentStateException ex,
                        HttpServletRequest request) {

                ErrorResponse response = new ErrorResponse(
                                Instant.now(),
                                404,
                                "INVALID_PAYMENT_STATE",
                                ex.getMessage(),
                                request.getRequestURI());

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(response);
        }

        @ExceptionHandler(PaymentProviderException.class)
        public ResponseEntity<ErrorResponse> handlePaymentProviderFailure(
                        PaymentProviderException ex,
                        HttpServletRequest request) {

                ErrorResponse response = new ErrorResponse(
                                Instant.now(),
                                400,
                                "BAD_PAYMENT_REQUEST",
                                ex.getMessage(),
                                request.getRequestURI());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(response);
        }

        @ExceptionHandler(InvalidWebhookSignatureException.class)
        public ResponseEntity<ErrorResponse> handleInvalidWebhookSignature(
                        PaymentProviderException ex,
                        HttpServletRequest request) {

                ErrorResponse response = new ErrorResponse(
                                Instant.now(),
                                401,
                                "INVALID_WEBHOOK_SIGNATURE",
                                ex.getMessage(),
                                request.getRequestURI());

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(response);
        }
}