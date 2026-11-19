
package com.ridex.location.integration;

import com.ridex.location.event.DriverStatusChangedEvent;
import com.ridex.location.service.DriverLocationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
                "spring.kafka.listener.auto-startup=false",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "management.health.redis.enabled=false"
})
@EnableAutoConfiguration(exclude = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class
})
class DriverLocationRedisIntegrationTest {

        private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                        .withExposedPorts(6379);

        /*
         * Start Redis before Spring evaluates @DynamicPropertySource.
         * This ensures getMappedPort(6379) is available.
         */
        static {
                REDIS.start();
        }

        @DynamicPropertySource
        static void redisProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.data.redis.host", REDIS::getHost);
                registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        }

        @Autowired
        @Qualifier("stringRedisTemplate")
        private RedisTemplate<String, String> redis;

        @Autowired
        private DriverLocationService service;

        @BeforeEach
        void clearKeys() {
                redis.delete("ridex:drivers:locations");
                redis.delete("ridex:drivers:available");
                redis.delete("ridex:drivers:last-seen");
                redis.delete("ridex:drivers:user-to-driver");
                redis.delete("ridex:drivers:status");
                redis.delete("ridex:processed-events");
        }

        @Test
        void storesLocationAndLastSeenInRealRedis() {
                UUID userId = UUID.randomUUID();
                UUID driverId = UUID.randomUUID();

                service.updateDriverIdentity(userId, driverId);
                service.updateDriverStatus(driverId, "AVAILABLE");
                service.updateDriverLocation(userId, 12.9716, 77.5946);

                var position = redis.opsForGeo()
                                .position("ridex:drivers:locations", driverId.toString());

                assertNotNull(position);
                assertNotNull(position.get(0));

                assertEquals(77.5946, position.get(0).getX(), 0.01);
                assertEquals(12.9716, position.get(0).getY(), 0.01);

                assertNotNull(
                                redis.opsForZSet()
                                                .score("ridex:drivers:last-seen", driverId.toString()));
        }

        @Test
        void availabilityEventUpdatesRealRedisSetAndDeduplicates() {
                UUID userId = UUID.randomUUID();
                UUID driverId = UUID.randomUUID();
                UUID eventId = UUID.randomUUID();

                DriverStatusChangedEvent event = new DriverStatusChangedEvent(
                                eventId,
                                driverId,
                                userId,
                                "AVAILABLE",
                                Instant.now());

                service.updateDriverAvailability(event);
                service.updateDriverAvailability(event);

                assertTrue(Boolean.TRUE.equals(
                                redis.opsForSet()
                                                .isMember("ridex:drivers:available", driverId.toString())));

                assertTrue(Boolean.TRUE.equals(
                                redis.opsForSet()
                                                .isMember("ridex:processed-events", eventId.toString())));
        }

        @Test
        void geoSearchFindsStoredDriverWithinRadius() {
                UUID driverId = UUID.randomUUID();

                redis.opsForGeo().add(
                                "ridex:drivers:locations",
                                new Point(77.5946, 12.9716),
                                driverId.toString());

                GeoResults<RedisGeoCommands.GeoLocation<String>> results = redis.opsForGeo().search(
                                "ridex:drivers:locations",
                                GeoReference.fromCoordinate(77.5946, 12.9716),
                                new Distance(2, Metrics.KILOMETERS),
                                RedisGeoCommands.GeoSearchCommandArgs
                                                .newGeoSearchArgs()
                                                .includeDistance()
                                                .sortAscending()
                                                .limit(100));

                assertNotNull(results);
                assertEquals(1, results.getContent().size());

                GeoResult<RedisGeoCommands.GeoLocation<String>> result = results.getContent().get(0);

                assertEquals(
                                driverId.toString(),
                                result.getContent().getName());
        }

        @Test
        void findNearbyDriversReturnsDriversWithinRadius() {
                UUID driverId = UUID.randomUUID();

                redis.opsForGeo().add(
                                "ridex:drivers:locations",
                                new Point(77.5946, 12.9716),
                                driverId.toString());

                redis.opsForSet().add(
                                "ridex:drivers:available",
                                driverId.toString());

                var nearbyDrivers = service.findNearbyDrivers(
                                12.9716,
                                77.5946,
                                2.0);

                assertNotNull(nearbyDrivers);
                assertEquals(1, nearbyDrivers.size());
                assertEquals(driverId, nearbyDrivers.get(0).driverId());
                assertTrue(nearbyDrivers.get(0).distanceKm() >= 0);
        }
}
