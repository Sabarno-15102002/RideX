package com.ridex.trip.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cache.Cache;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(MockitoExtension.class)
class TripCacheErrorHandlerTest {

    private TripCacheErrorHandler errorHandler;

    @Mock
    private Cache cache;

    @BeforeEach
    void setUp() {
        errorHandler = new TripCacheErrorHandler();

        when(cache.getName()).thenReturn("tripPayment");
    }

    // ---------------------------------------------------------
    // GET ERROR
    // ---------------------------------------------------------

    @Test
    void handleCacheGetError_shouldNotThrowException() {

        RuntimeException exception = new RuntimeException("Redis GET failed");

        assertDoesNotThrow(() -> errorHandler.handleCacheGetError(
                exception,
                cache,
                "trip-123"));

        verify(cache, times(1)).getName();
    }

    @Test
    void handleCacheGetError_shouldHandleNullKey() {

        RuntimeException exception = new RuntimeException("Redis GET failed");

        assertDoesNotThrow(() -> errorHandler.handleCacheGetError(
                exception,
                cache,
                null));

        verify(cache, times(1)).getName();
    }

    // ---------------------------------------------------------
    // PUT ERROR
    // ---------------------------------------------------------

    @Test
    void handleCachePutError_shouldNotThrowException() {

        RuntimeException exception = new RuntimeException("Redis PUT failed");

        Object key = "trip-123";
        Object value = "SUCCESS";

        assertDoesNotThrow(() -> errorHandler.handleCachePutError(
                exception,
                cache,
                key,
                value));

        verify(cache, times(1)).getName();
    }

    @Test
    void handleCachePutError_shouldHandleNullKeyAndValue() {

        RuntimeException exception = new RuntimeException("Redis PUT failed");

        assertDoesNotThrow(() -> errorHandler.handleCachePutError(
                exception,
                cache,
                null,
                null));

        verify(cache, times(1)).getName();
    }

    // ---------------------------------------------------------
    // EVICT ERROR
    // ---------------------------------------------------------

    @Test
    void handleCacheEvictError_shouldNotThrowException() {

        RuntimeException exception = new RuntimeException("Redis EVICT failed");

        assertDoesNotThrow(() -> errorHandler.handleCacheEvictError(
                exception,
                cache,
                "trip-123"));

        verify(cache, times(1)).getName();
    }

    @Test
    void handleCacheEvictError_shouldHandleNullKey() {

        RuntimeException exception = new RuntimeException("Redis EVICT failed");

        assertDoesNotThrow(() -> errorHandler.handleCacheEvictError(
                exception,
                cache,
                null));

        verify(cache, times(1)).getName();
    }

    // ---------------------------------------------------------
    // CLEAR ERROR
    // ---------------------------------------------------------

    @Test
    void handleCacheClearError_shouldNotThrowException() {

        RuntimeException exception = new RuntimeException("Redis CLEAR failed");

        assertDoesNotThrow(() -> errorHandler.handleCacheClearError(
                exception,
                cache));

        verify(cache, times(1)).getName();
    }
}
