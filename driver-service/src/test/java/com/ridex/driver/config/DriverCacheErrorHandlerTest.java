package com.ridex.driver.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class DriverCacheErrorHandlerTest {

    private DriverCacheErrorHandler handler;
    private Cache cache;
    private RuntimeException exception;

    @BeforeEach
    void setUp() {
        handler = new DriverCacheErrorHandler();
        cache = mock(Cache.class);
        when(cache.getName()).thenReturn("driverProfile");
        exception = new RuntimeException("Redis unavailable");
    }

    @Test
    void handleCacheGetError_shouldNotPropagateException() {
        assertDoesNotThrow(() -> handler.handleCacheGetError(exception, cache, "driver-1"));
    }

    @Test
    void handleCachePutError_shouldNotPropagateException() {
        assertDoesNotThrow(() -> handler.handleCachePutError(
                exception, cache, "driver-1", "value"));
    }

    @Test
    void handleCacheEvictError_shouldNotPropagateException() {
        assertDoesNotThrow(() -> handler.handleCacheEvictError(exception, cache, "driver-1"));
    }

    @Test
    void handleCacheClearError_shouldNotPropagateException() {
        assertDoesNotThrow(() -> handler.handleCacheClearError(exception, cache));
    }
}
