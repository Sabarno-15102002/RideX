package com.ridex.rider.exception;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ridex.rider.dto.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(RiderNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleRiderNotFound(
                        RiderNotFoundException ex,
                        HttpServletRequest request) {

                return build(
                                HttpStatus.NOT_FOUND,
                                "RIDER_NOT_FOUND",
                                ex.getMessage(),
                                request);
        }

        @ExceptionHandler(SavedLocationNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleLocationNotFound(
                        SavedLocationNotFoundException ex,
                        HttpServletRequest request) {

                return build(
                                HttpStatus.NOT_FOUND,
                                "SAVED_LOCATION_NOT_FOUND",
                                ex.getMessage(),
                                request);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                Map<String, String> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .collect(Collectors.toMap(
                                                error -> error.getField(),
                                                error -> error.getDefaultMessage(),
                                                (first, second) -> first));

                return build(
                                HttpStatus.BAD_REQUEST,
                                "VALIDATION_ERROR",
                                errors.toString(),
                                request);
        }

        @ExceptionHandler(EventSerializationException.class)
        public ResponseEntity<ErrorResponse> handleEventSerializationException(
                        EventSerializationException ex,
                        HttpServletRequest request) {
                ErrorResponse response = new ErrorResponse(
                                Instant.now(),
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "EVENT_SERIALIZATION_ERROR",
                                ex.getMessage(),
                                request.getRequestURI(),
                                request.getHeader("X-Correlation-Id"));
                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(response);
        }

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleInvalidCredentials(
                        InvalidCredentialsException ex,
                        HttpServletRequest request) {

                ErrorResponse response = new ErrorResponse(
                                Instant.now(),
                                401,
                                "INVALID_CREDENTIALS",
                                ex.getMessage(),
                                request.getRequestURI(),
                                request.getHeader("X-Correlation-Id"));

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(response);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                Instant.now(),
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "INTERNAL_SERVER_ERROR",
                                ex.getMessage(),
                                request.getRequestURI(),
                                request.getHeader("X-Correlation-Id"));

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(errorResponse);
        }

        private ResponseEntity<ErrorResponse> build(
                        HttpStatus status,
                        String code,
                        String message,
                        HttpServletRequest request) {

                ErrorResponse response = new ErrorResponse(
                                Instant.now(),
                                status.value(),
                                code,
                                message,
                                request.getRequestURI(),
                                request.getHeader("X-Correlation-Id"));

                return ResponseEntity
                                .status(status)
                                .body(response);
        }
}