package com.ridex.pricing.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(
            ResourceNotFoundException exception) {

        return Map.of(
                "timestamp", Instant.now(),
                "status", 404,
                "error", "NOT_FOUND",
                "message", exception.getMessage()
        );
    }
}