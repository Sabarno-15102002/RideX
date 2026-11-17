package com.ridex.rider.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

class RiderCacheErrorHandlerTest {

    private RiderCacheErrorHandler errorHandler;
    private Cache cache;
    private RuntimeException exception;

    @BeforeEach
    void setUp() {
        errorHandler = new RiderCacheErrorHandler();
        cache = mock(Cache.class);
        when(cache.getName()).thenReturn("riderProfile");
        exception = new RuntimeException("Redis unavailable");
    }

    @Test
    void handleCacheGetError_shouldNotPropagateException() {
        assertDoesNotThrow(() -> errorHandler.handleCacheGetError(
                exception,
                cache,
                "rider-123"));

        verify(cache).getName();
    }

    @Test
    void handleCachePutError_shouldNotPropagateException() {
        assertDoesNotThrow(() -> errorHandler.handleCachePutError(
                exception,
                cache,
                "rider-123",
                "profile"));

        verify(cache).getName();
    }

    @Test
    void handleCacheEvictError_shouldNotPropagateException() {
        assertDoesNotThrow(() -> errorHandler.handleCacheEvictError(
                exception,
                cache,
                "rider-123"));

        verify(cache).getName();
    }

    @Test
    void handleCacheClearError_shouldNotPropagateException() {
        assertDoesNotThrow(() -> errorHandler.handleCacheClearError(
                exception,
                cache));

        verify(cache).getName();
    }
}
