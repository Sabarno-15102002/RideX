package com.ridex.driver.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ridex.driver.dto.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(ResourceAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleResourceAlreadyExists(
                        ResourceAlreadyExistsException ex,
                        HttpServletRequest request) {

                ErrorResponse response = new ErrorResponse(
                                Instant.now(),
                                409,
                                "RESOURCE_ALREADY_EXISTS",
                                ex.getMessage(),
                                request.getRequestURI(),
                                request.getHeader("X-Correlation-Id"));

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(response);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(
                        ResourceNotFoundException ex,
                        HttpServletRequest request) {

                ErrorResponse response = new ErrorResponse(
                                Instant.now(),
                                404,
                                "RESOURCE_NOT_FOUND",
                                ex.getMessage(),
                                request.getRequestURI(),
                                request.getHeader("X-Correlation-Id"));

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(response);
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

        @ExceptionHandler(TripStateException.class)
        public ResponseEntity<ErrorResponse> handleTripStateException(
                        TripStateException ex,
                        HttpServletRequest request) {

                ErrorResponse response = new ErrorResponse(
                                Instant.now(),
                                400,
                                "INVALID_STATE_IN_TRIP",
                                ex.getMessage(),
                                request.getRequestURI(),
                                request.getHeader("X-Correlation-Id"));

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(response);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {

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

}
