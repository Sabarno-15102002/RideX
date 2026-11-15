package com.ridex.trip.config;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = RedisCacheIntegrationTest.TestConfig.class)
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class RedisCacheIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("test.redis.host", REDIS::getHost);
        registry.add(
                "test.redis.port",
                () -> REDIS.getMappedPort(6379));
    }

    @Configuration
    @Import({
            CacheConfig.class,
            TripCacheErrorHandler.class
    })
    static class TestConfig {

        @Bean
        RedisConnectionFactory redisConnectionFactory(
                @Value("${test.redis.host}") String host,
                @Value("${test.redis.port}") int port) {
            return new LettuceConnectionFactory(host, port);
        }

        @Bean
        CacheProbeService cacheProbeService() {
            return new CacheProbeService();
        }
    }

    static class CacheProbeService {

        private final AtomicInteger invocationCount = new AtomicInteger();

        @Cacheable(cacheNames = "tripStatus", key = "#p0")
        public String getTripStatus(String tripId) {
            invocationCount.incrementAndGet();
            return "DRIVER_ASSIGNED";
        }

        int getInvocationCount() {
            return invocationCount.get();
        }

        void reset() {
            invocationCount.set(0);
        }
    }

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CacheProbeService cacheProbeService;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });

        cacheProbeService.reset();
    }

    @Test
    void redis_shouldBeReachable() {
        assertNotNull(redisConnectionFactory.getConnection()
                .ping());
    }

    @Test
    void cacheMiss_shouldInvokeUnderlyingService() {
        String tripId = "trip-100";

        String status = cacheProbeService.getTripStatus(tripId);

        assertEquals("DRIVER_ASSIGNED", status);
        assertEquals(1, cacheProbeService.getInvocationCount());
    }

    @Test
    void cacheHit_shouldNotInvokeUnderlyingServiceAgain() {
        String tripId = "trip-200";

        String first = cacheProbeService.getTripStatus(tripId);
        String second = cacheProbeService.getTripStatus(tripId);

        assertEquals(first, second);
        assertEquals(1, cacheProbeService.getInvocationCount());
    }

    @Test
    void differentKeys_shouldHaveSeparateCacheEntries() {
        String first = cacheProbeService.getTripStatus("trip-300");
        String second = cacheProbeService.getTripStatus("trip-400");

        assertEquals(first, second);
        assertEquals(2, cacheProbeService.getInvocationCount());
    }

    @Test
    void cache_shouldStoreAndRetrieveSerializedValues() {
        Cache cache = cacheManager.getCache("tripStatus");

        assertNotNull(cache);

        cache.put("trip-500", "TRIP_COMPLETED");

        String cachedValue = cache.get("trip-500", String.class);

        assertEquals("TRIP_COMPLETED", cachedValue);
    }

    @Test
    void cacheEviction_shouldRemoveCachedEntry() {
        String tripId = "trip-600";

        // First call: cache miss.
        String first = cacheProbeService.getTripStatus(tripId);

        assertEquals("DRIVER_ASSIGNED", first);
        assertEquals(1, cacheProbeService.getInvocationCount());

        // Evict the entry.
        Cache cache = cacheManager.getCache("tripStatus");
        assertNotNull(cache);

        cache.evict(tripId);

        // Second call: should be another cache miss.
        String second = cacheProbeService.getTripStatus(tripId);

        assertEquals("DRIVER_ASSIGNED", second);
        assertEquals(2, cacheProbeService.getInvocationCount());
    }

}
