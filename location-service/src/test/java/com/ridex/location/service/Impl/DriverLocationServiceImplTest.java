package com.ridex.location.service.Impl;

import com.ridex.location.dto.request.DriverLocationUpdate;
import com.ridex.location.dto.response.NearbyDriverResponse;
import com.ridex.location.event.DriverStatusChangedEvent;
import com.ridex.location.event.TripCompletedEvent;
import com.ridex.location.exception.IllegalOperationException;
import com.ridex.location.exception.InvalidCredentialsException;
import com.ridex.location.exception.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DriverLocationServiceImplTest {

    private RedisTemplate<String, String> redis;
    private HashOperations<String, Object, Object> hash;
    private GeoOperations<String, String> geo;
    private ZSetOperations<String, String> zset;
    private SetOperations<String, String> set;
    private SimpMessagingTemplate messaging;

    private DriverLocationServiceImpl service;

    private UUID userId;
    private UUID driverId;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redis = mock(RedisTemplate.class);
        hash = mock(HashOperations.class);
        geo = mock(GeoOperations.class);
        zset = mock(ZSetOperations.class);
        set = mock(SetOperations.class);
        messaging = mock(SimpMessagingTemplate.class);

        when(redis.opsForHash()).thenReturn(hash);
        when(redis.opsForGeo()).thenReturn(geo);
        when(redis.opsForZSet()).thenReturn(zset);
        when(redis.opsForSet()).thenReturn(set);

        service = new DriverLocationServiceImpl(redis, messaging);

        userId = UUID.randomUUID();
        driverId = UUID.randomUUID();
    }

    // ---------------------------------------------------------
    // updateDriverLocation()
    // ---------------------------------------------------------

    @Test
    void updateLocationStoresGeoAndLastSeenAndBroadcasts() {
        when(hash.get("ridex:drivers:user-to-driver", userId.toString()))
                .thenReturn(driverId.toString());

        when(hash.get("ridex:drivers:status", driverId.toString()))
                .thenReturn("AVAILABLE");

        when(geo.add(
                eq("ridex:drivers:locations"),
                any(Point.class),
                eq(driverId.toString()))).thenReturn(1L);

        service.updateDriverLocation(userId, 12.5, 77.6);

        verify(geo).add(
                eq("ridex:drivers:locations"),
                argThat(point -> point.getX() == 77.6 &&
                        point.getY() == 12.5),
                eq(driverId.toString()));

        verify(zset).add(
                eq("ridex:drivers:last-seen"),
                eq(driverId.toString()),
                anyDouble());

        verify(messaging).convertAndSend(
                eq("/topic/drivers/" + driverId + "/location"),
                any(DriverLocationUpdate.class));
    }

    @Test
    void updateLocationWithoutDriverMappingIsUnauthorized() {
        when(hash.get("ridex:drivers:user-to-driver", userId.toString()))
                .thenReturn(null);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.updateDriverLocation(userId, 1, 2));

        verifyNoInteractions(geo, messaging);
    }

    @Test
    void updateLocationWithoutStatusIsNotFound() {
        when(hash.get("ridex:drivers:user-to-driver", userId.toString()))
                .thenReturn(driverId.toString());

        when(hash.get("ridex:drivers:status", driverId.toString()))
                .thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.updateDriverLocation(userId, 1, 2));

        verifyNoInteractions(geo, messaging);
    }

    @Test
    void updateLocationRejectsOfflineDriver() {
        when(hash.get("ridex:drivers:user-to-driver", userId.toString()))
                .thenReturn(driverId.toString());

        when(hash.get("ridex:drivers:status", driverId.toString()))
                .thenReturn("OFFLINE");

        assertThrows(
                IllegalOperationException.class,
                () -> service.updateDriverLocation(userId, 1, 2));

        verifyNoInteractions(geo, messaging);
    }

    @Test
    void updateLocationRejectsUnknownDriverStatus() {
        when(hash.get("ridex:drivers:user-to-driver", userId.toString()))
                .thenReturn(driverId.toString());

        when(hash.get("ridex:drivers:status", driverId.toString()))
                .thenReturn("SUSPENDED");

        assertThrows(
                IllegalOperationException.class,
                () -> service.updateDriverLocation(userId, 1, 2));

        verifyNoInteractions(geo, messaging);
    }

    @Test
    void updateLocationAllowsOnTripDriver() {
        when(hash.get("ridex:drivers:user-to-driver", userId.toString()))
                .thenReturn(driverId.toString());

        when(hash.get("ridex:drivers:status", driverId.toString()))
                .thenReturn("ON_TRIP");

        service.updateDriverLocation(userId, 1, 2);

        verify(geo).add(
                eq("ridex:drivers:locations"),
                any(Point.class),
                eq(driverId.toString()));

        verify(messaging).convertAndSend(
                eq("/topic/drivers/" + driverId + "/location"),
                any(DriverLocationUpdate.class));
    }

    // ---------------------------------------------------------
    // findNearbyDrivers()
    // ---------------------------------------------------------

    @Test
    void nearbySearchReturnsEmptyWhenGeoSearchIsNull() {
        when(geo.search(
                eq("ridex:drivers:locations"),
                any(GeoReference.class),
                any(Distance.class),
                any(RedisGeoCommands.GeoSearchCommandArgs.class))).thenReturn(null);

        assertTrue(service.findNearbyDrivers(1, 2, 5).isEmpty());

        verifyNoInteractions(set);
    }

    @Test
    void nearbySearchReturnsEmptyWhenAvailableDriversSetIsEmpty() {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = new GeoResults<>(
                List.of(),
                new Distance(0, Metrics.KILOMETERS));

        when(geo.search(
                eq("ridex:drivers:locations"),
                any(GeoReference.class),
                any(Distance.class),
                any(RedisGeoCommands.GeoSearchCommandArgs.class))).thenReturn(results);

        when(set.members("ridex:drivers:available"))
                .thenReturn(Collections.emptySet());

        assertTrue(service.findNearbyDrivers(1, 2, 5).isEmpty());
    }

    @Test
    void nearbySearchReturnsEmptyWhenAvailableDriversSetIsNull() {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = new GeoResults<>(
                List.of(),
                new Distance(0, Metrics.KILOMETERS));

        when(geo.search(
                eq("ridex:drivers:locations"),
                any(GeoReference.class),
                any(Distance.class),
                any(RedisGeoCommands.GeoSearchCommandArgs.class))).thenReturn(results);

        when(set.members("ridex:drivers:available"))
                .thenReturn(null);

        assertTrue(service.findNearbyDrivers(1, 2, 5).isEmpty());
    }

    @Test
    void nearbySearchReturnsMappedDriverWithDistance() {
        UUID nearbyDriverId = UUID.randomUUID();

        RedisGeoCommands.GeoLocation<String> location = new RedisGeoCommands.GeoLocation<>(
                nearbyDriverId.toString(),
                new Point(77.5946, 12.9716));

        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult = new GeoResult<>(
                location,
                new Distance(0.45, Metrics.KILOMETERS));

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = new GeoResults<>(
                List.of(geoResult),
                new Distance(0.45, Metrics.KILOMETERS));

        when(geo.search(
                eq("ridex:drivers:locations"),
                any(GeoReference.class),
                any(Distance.class),
                any(RedisGeoCommands.GeoSearchCommandArgs.class))).thenReturn(results);

        when(set.members("ridex:drivers:available"))
                .thenReturn(Set.of(nearbyDriverId.toString()));

        List<NearbyDriverResponse> nearbyDrivers = service.findNearbyDrivers(12.9716, 77.5946, 2);

        assertNotNull(nearbyDrivers);
        assertEquals(1, nearbyDrivers.size());

        NearbyDriverResponse response = nearbyDrivers.get(0);

        assertEquals(nearbyDriverId, response.driverId());
        assertEquals(0.45, response.distanceKm(), 0.001);
    }

    @Test
    void nearbySearchMapsMultipleDrivers() {
        UUID firstDriverId = UUID.randomUUID();
        UUID secondDriverId = UUID.randomUUID();

        GeoResult<RedisGeoCommands.GeoLocation<String>> firstResult = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>(
                        firstDriverId.toString(),
                        new Point(77.5946, 12.9716)),
                new Distance(0.25, Metrics.KILOMETERS));

        GeoResult<RedisGeoCommands.GeoLocation<String>> secondResult = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>(
                        secondDriverId.toString(),
                        new Point(77.5950, 12.9720)),
                new Distance(0.75, Metrics.KILOMETERS));

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = new GeoResults<>(
                List.of(firstResult, secondResult),
                new Distance(0.75, Metrics.KILOMETERS));

        when(geo.search(
                eq("ridex:drivers:locations"),
                any(GeoReference.class),
                any(Distance.class),
                any(RedisGeoCommands.GeoSearchCommandArgs.class))).thenReturn(results);

        when(set.members("ridex:drivers:available"))
                .thenReturn(Set.of(
                        firstDriverId.toString(),
                        secondDriverId.toString()));

        List<NearbyDriverResponse> nearbyDrivers = service.findNearbyDrivers(12.9716, 77.5946, 2);

        assertEquals(2, nearbyDrivers.size());

        assertEquals(firstDriverId, nearbyDrivers.get(0).driverId());
        assertEquals(0.25, nearbyDrivers.get(0).distanceKm(), 0.001);

        assertEquals(secondDriverId, nearbyDrivers.get(1).driverId());
        assertEquals(0.75, nearbyDrivers.get(1).distanceKm(), 0.001);
    }

    // ---------------------------------------------------------
    // updateDriverAvailability()
    // ---------------------------------------------------------

    @Test
    void updateAvailabilityAddsAvailableDriverAndMarksEventProcessed() {
        UUID eventId = UUID.randomUUID();

        DriverStatusChangedEvent event = new DriverStatusChangedEvent(
                eventId,
                driverId,
                userId,
                "AVAILABLE",
                Instant.now());

        when(set.isMember(
                "ridex:processed-events",
                eventId.toString())).thenReturn(false);

        service.updateDriverAvailability(event);

        verify(set).add(
                "ridex:drivers:available",
                driverId.toString());

        verify(set).add(
                "ridex:processed-events",
                eventId.toString());
    }

    @Test
    void updateAvailabilityRemovesUnavailableDriver() {
        UUID eventId = UUID.randomUUID();

        DriverStatusChangedEvent event = new DriverStatusChangedEvent(
                eventId,
                driverId,
                userId,
                "ON_TRIP",
                Instant.now());

        when(set.isMember(
                "ridex:processed-events",
                eventId.toString())).thenReturn(false);

        service.updateDriverAvailability(event);

        verify(set).remove(
                "ridex:drivers:available",
                driverId.toString());

        verify(set).add(
                "ridex:processed-events",
                eventId.toString());
    }

    @Test
    void duplicateAvailabilityEventIsIgnored() {
        UUID eventId = UUID.randomUUID();

        DriverStatusChangedEvent event = new DriverStatusChangedEvent(
                eventId,
                driverId,
                userId,
                "AVAILABLE",
                Instant.now());

        when(set.isMember(
                "ridex:processed-events",
                eventId.toString())).thenReturn(true);

        service.updateDriverAvailability(event);

        verify(set, never()).add(
                "ridex:drivers:available",
                driverId.toString());

        verify(set, never()).remove(
                "ridex:drivers:available",
                driverId.toString());
    }

    @Test
    void updateAvailabilityTreatsNullProcessedFlagAsNotProcessed() {
        UUID eventId = UUID.randomUUID();

        DriverStatusChangedEvent event = new DriverStatusChangedEvent(
                eventId,
                driverId,
                userId,
                "AVAILABLE",
                Instant.now());

        when(set.isMember(
                "ridex:processed-events",
                eventId.toString())).thenReturn(null);

        service.updateDriverAvailability(event);

        verify(set).add(
                "ridex:drivers:available",
                driverId.toString());

        verify(set).add(
                "ridex:processed-events",
                eventId.toString());
    }

    // ---------------------------------------------------------
    // updateDriverIdentity()
    // ---------------------------------------------------------

    @Test
    void updateIdentityWritesHashMapping() {
        service.updateDriverIdentity(userId, driverId);

        verify(hash).put(
                "ridex:drivers:user-to-driver",
                userId.toString(),
                driverId.toString());
    }

    // ---------------------------------------------------------
    // updateDriverStatus()
    // ---------------------------------------------------------

    @Test
    void updateStatusWritesHashValue() {
        service.updateDriverStatus(driverId, "AVAILABLE");

        verify(hash).put(
                "ridex:drivers:status",
                driverId.toString(),
                "AVAILABLE");
    }

    // ---------------------------------------------------------
    // handleTripCompleted()
    // ---------------------------------------------------------

    @Test
    void tripCompletedChecksProcessedEvent() {
        UUID eventId = UUID.randomUUID();

        TripCompletedEvent event = new TripCompletedEvent(
                eventId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                driverId,
                1,
                2,
                3,
                4,
                Instant.now(),
                Instant.now());

        when(set.isMember(
                "ridex:processed-events",
                eventId.toString())).thenReturn(false);

        assertDoesNotThrow(() -> service.handleTripCompleted(event));

        verify(set).isMember(
                "ridex:processed-events",
                eventId.toString());
    }

    @Test
    void duplicateTripCompletedEventDoesNotThrow() {
        UUID eventId = UUID.randomUUID();

        TripCompletedEvent event = new TripCompletedEvent(
                eventId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                driverId,
                1,
                2,
                3,
                4,
                Instant.now(),
                Instant.now());

        when(set.isMember(
                "ridex:processed-events",
                eventId.toString())).thenReturn(true);

        assertDoesNotThrow(() -> service.handleTripCompleted(event));

        verify(set).isMember(
                "ridex:processed-events",
                eventId.toString());
    }
}
