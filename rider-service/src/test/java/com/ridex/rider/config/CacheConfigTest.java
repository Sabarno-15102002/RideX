package com.ridex.rider.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class CacheConfigTest {

    private CacheConfig cacheConfig;
    private RiderCacheErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        errorHandler = new RiderCacheErrorHandler();
        cacheConfig = new CacheConfig(errorHandler);
    }

    @Test
    void cacheManager_shouldCreateRedisCacheManager() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        RedisCacheManager cacheManager = cacheConfig.cacheManager(connectionFactory);

        assertNotNull(cacheManager);
        assertNotNull(cacheManager.getCache("riderProfile"));
    }

    @Test
    void errorHandler_shouldReturnConfiguredErrorHandler() {
        assertSame(errorHandler, cacheConfig.errorHandler());
    }
}
