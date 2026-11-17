package com.ridex.driver.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CacheConfigTest {

    private DriverCacheErrorHandler errorHandler;
    private CacheConfig cacheConfig;
    private RedisConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        errorHandler = new DriverCacheErrorHandler();
        cacheConfig = new CacheConfig(errorHandler);
        connectionFactory = mock(RedisConnectionFactory.class);
    }

    @Test
    void cacheManager_shouldCreateRedisCacheManager() {
        RedisCacheManager manager =
                cacheConfig.cacheManager(connectionFactory);

        assertNotNull(manager);
    }

    @Test
    void cacheManager_shouldConfigureAllDriverCaches() {
        RedisCacheManager manager =
                cacheConfig.cacheManager(connectionFactory);

        Cache profileCache = manager.getCache("driverProfile");
        Cache ratingCache = manager.getCache("driverRating");
        Cache vehicleCache = manager.getCache("vehicleDetails");

        assertNotNull(profileCache);
        assertNotNull(ratingCache);
        assertNotNull(vehicleCache);

        assertEquals("driverProfile", profileCache.getName());
        assertEquals("driverRating", ratingCache.getName());
        assertEquals("vehicleDetails", vehicleCache.getName());
    }

    @Test
    void errorHandler_shouldReturnConfiguredHandler() {
        assertSame(errorHandler, cacheConfig.errorHandler());
    }
}
