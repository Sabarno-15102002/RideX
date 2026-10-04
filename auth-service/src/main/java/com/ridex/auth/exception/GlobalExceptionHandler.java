package com.ridex.auth.exception;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ridex.auth.dto.response.ErrorResponse;

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

        @ExceptionHandler(InvalidRefreshTokenException.class)
        public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
                        InvalidRefreshTokenException ex,
                        HttpServletRequest request) {

                ErrorResponse response = new ErrorResponse(
                                Instant.now(),
                                HttpStatus.UNAUTHORIZED.value(),
                                "INVALID_REFRESH_TOKEN",
                                ex.getMessage(),
                                request.getRequestURI(),
                                request.getHeader("X-Correlation-Id"));

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
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

        @ExceptionHandler(InvalidAlgorithmParameterException.class)
        public ResponseEntity<ErrorResponse> handleInvalidAlgorithmParameterException(
                        InvalidAlgorithmParameterException ex,
                        HttpServletRequest request) {
                ErrorResponse response = new ErrorResponse(
                                Instant.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                "INVALID_ALGORITHM_PARAMETER",
                                ex.getMessage(),
                                request.getRequestURI(),
                                request.getHeader("X-Correlation-Id"));
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
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

}