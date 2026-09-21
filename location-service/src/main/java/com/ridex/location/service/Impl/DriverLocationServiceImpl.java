package com.ridex.location.service.Impl;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.ridex.location.dto.request.DriverLocationUpdate;
import com.ridex.location.dto.response.NearbyDriverResponse;
import com.ridex.location.event.DriverStatusChangedEvent;
import com.ridex.location.event.TripCompletedEvent;
import com.ridex.location.service.DriverLocationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverLocationServiceImpl implements DriverLocationService {

        private static final String DRIVER_LOCATIONS_KEY = "ridex:drivers:locations";
        private static final String AVAILABLE_DRIVERS_KEY = "ridex:drivers:available";
        private static final String PROCESSED_EVENTS_KEY = "ridex:processed-events";
        private static final String DRIVER_LAST_SEEN_KEY = "ridex:drivers:last-seen";
        private static final String USER_TO_DRIVER_KEY = "ridex:drivers:user-to-driver";
        private static final String DRIVER_STATUS_KEY = "ridex:drivers:status";

        private final RedisTemplate<String, String> redisTemplate;
        private final SimpMessagingTemplate messagingTemplate;

        @Override
        public void updateDriverLocation(UUID userId, double latitude, double longitude) {

                Object driverIdValue = redisTemplate.opsForHash()
                                .get(
                                                USER_TO_DRIVER_KEY,
                                                userId.toString());

                if (driverIdValue == null) {
                        throw new IllegalArgumentException("Driver profile not found for authenticated user");
                }

                UUID driverId = UUID.fromString(driverIdValue.toString());

                String status = getDriverStatus(driverId);

                if (!"AVAILABLE".equals(status)
                                && !"ON_TRIP".equals(status)) {

                        throw new IllegalStateException(
                                        "Driver is not allowed to update location in status: "
                                                        + status);
                }

                Long result = redisTemplate.opsForGeo()
                                .add(
                                                DRIVER_LOCATIONS_KEY,
                                                new Point(longitude, latitude),
                                                driverId.toString());

                redisTemplate.opsForZSet().add(
                                DRIVER_LAST_SEEN_KEY,
                                driverId.toString(),
                                System.currentTimeMillis());

                log.info(
                                "Redis GEO update: driverId={}, latitude={}, longitude={}, result={}",
                                driverId,
                                latitude,
                                longitude,
                                result);

                messagingTemplate.convertAndSend(
                                "/topic/drivers/" + driverId + "/location",
                                new DriverLocationUpdate(
                                                driverId,
                                                latitude,
                                                longitude,
                                                Instant.now()));
                log.info(
                                "WebSocket message sent: driverId={}, latitude={}, longitude={}",
                                driverId,
                                latitude,
                                longitude);
        }

        @Override
        public List<NearbyDriverResponse> findNearbyDrivers(
                        double latitude,
                        double longitude,
                        double radiusKm) {
                RedisGeoCommands.GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs
                                .newGeoSearchArgs()
                                .includeDistance()
                                .sortAscending()
                                .limit(100);

                GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().search(
                                DRIVER_LOCATIONS_KEY,
                                GeoReference.fromCoordinate(longitude, latitude),
                                new Distance(radiusKm, Metrics.KILOMETERS),
                                args);

                if (results == null) {
                        return List.of();
                }

                Set<String> availableDriverIds = redisTemplate.opsForSet()
                                .members(AVAILABLE_DRIVERS_KEY);

                if (availableDriverIds == null || availableDriverIds.isEmpty()) {
                        return List.of();
                }

                return results.getContent()
                                .stream()
                                .map(result -> new NearbyDriverResponse(
                                                UUID.fromString(result.getContent().getName()),
                                                result.getDistance().getValue()))
                                .toList();
        }

        @Override
        public void updateDriverAvailability(DriverStatusChangedEvent event) {
                Boolean alreadyProcessed = redisTemplate.opsForSet().isMember(PROCESSED_EVENTS_KEY,
                                event.eventId().toString());

                if (Boolean.TRUE.equals(alreadyProcessed)) {
                        return;
                }

                boolean available = "AVAILABLE".equals(event.status());
                if (available) {
                        redisTemplate.opsForSet()
                                        .add(AVAILABLE_DRIVERS_KEY, event.driverId().toString());

                        log.info("Driver marked AVAILABLE in Redis: driverId={}", event.driverId());
                } else {
                        redisTemplate.opsForSet()
                                        .remove(AVAILABLE_DRIVERS_KEY, event.driverId().toString());

                        log.info("Driver removed from AVAILABLE set: driverId={}", event.driverId());
                }
                redisTemplate.opsForSet().add(PROCESSED_EVENTS_KEY, event.eventId().toString());
                log.info("Event processed: eventId={}", event.eventId());
        }

        @Override
        public void updateDriverIdentity(UUID userId, UUID driverId) {
                redisTemplate.opsForHash().put(
                                USER_TO_DRIVER_KEY,
                                userId.toString(),
                                driverId.toString());

                log.info(
                                "Updated user-to-driver mapping: userId={}, driverId={}",
                                userId,
                                driverId);
        }

        @Override
        public void updateDriverStatus(
                        UUID driverId,
                        String status) {
                redisTemplate.opsForHash().put(
                                DRIVER_STATUS_KEY,
                                driverId.toString(),
                                status);

                log.info(
                                "Updated driver status in Redis: driverId={}, status={}",
                                driverId,
                                status);
        }

        private String getDriverStatus(UUID driverId) {
                Object status = redisTemplate.opsForHash()
                                .get(
                                                DRIVER_STATUS_KEY,
                                                driverId.toString());

                if (status == null) {
                        throw new IllegalStateException(
                                        "Driver status not available");
                }

                return status.toString();
        }

        @Override 
        public void handleTripCompleted(TripCompletedEvent event) {

                Boolean alreadyProcessed = redisTemplate.opsForSet().isMember(PROCESSED_EVENTS_KEY,
                                event.eventId().toString());

                if (Boolean.TRUE.equals(alreadyProcessed)) {
                        // return;
                }
        }
}