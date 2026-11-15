package com.ridex.trip.service.util;

import com.ridex.trip.config.CacheConfig;
import com.ridex.trip.config.TripCacheErrorHandler;
import com.ridex.trip.dto.PaymentStatus;
import com.ridex.trip.entity.TripPayment;
import com.ridex.trip.event.event.PaymentCreatedEvent;
import com.ridex.trip.event.event.PaymentFailedEvent;
import com.ridex.trip.event.event.PaymentRefundedEvent;
import com.ridex.trip.event.event.PaymentSucceededEvent;
import com.ridex.trip.exception.IllegalOperationException;
import com.ridex.trip.repository.TripPaymentRepository;
import com.ridex.trip.service.TripPaymentProjection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(classes = TripPaymentRedisIntegrationTest.TestConfig.class)
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class TripPaymentRedisIntegrationTest {

        @Container
        static final GenericContainer<?> REDIS = new GenericContainer<>(
                        DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);

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
                                org.springframework.core.env.Environment environment) {
                        return new LettuceConnectionFactory(
                                        environment.getProperty("test.redis.host"),
                                        environment.getProperty("test.redis.port", Integer.class));
                }

                @Bean
                TripPaymentRepository tripPaymentRepository() {
                        return mock(TripPaymentRepository.class);
                }

                @Bean
                TripPaymentQueryService tripPaymentQueryService(
                                TripPaymentRepository repository) {
                        return new TripPaymentQueryService(repository);
                }

                @Bean
                TripPaymentProjection tripPaymentProjection(
                                TripPaymentRepository repository) {
                        return new TripPaymentProjection(repository);
                }
        }

        @Autowired
        private TripPaymentQueryService paymentQueryService;

        @Autowired
        private TripPaymentRepository repository;

        @Autowired
        private TripPaymentProjection paymentProjection;

        @Autowired
        private CacheManager cacheManager;

        private UUID tripId;
        private UUID riderId;
        private UUID paymentId;

        private TripPayment payment;

        @BeforeEach
        void setUp() {
                Cache cache = cacheManager.getCache("tripPayment");

                assertNotNull(cache);
                cache.clear();

                reset(repository);

                tripId = UUID.randomUUID();
                riderId = UUID.randomUUID();
                paymentId = UUID.randomUUID();

                payment = new TripPayment(
                                tripId,
                                paymentId,
                                riderId,
                                new BigDecimal("250.00"),
                                "INR",
                                PaymentStatus.SUCCESS,
                                Instant.now());

                when(repository.findById(tripId))
                                .thenReturn(Optional.of(payment));
        }

        @Test
        void getPaymentForTrip_shouldFetchFromRepositoryOnCacheMiss() {
                var response = paymentQueryService.getPaymentForTrip(
                                tripId,
                                riderId);

                assertNotNull(response);
                assertEquals(tripId, response.tripId());
                assertEquals(paymentId, response.paymentId());
                assertEquals(PaymentStatus.SUCCESS, response.status());

                verify(repository, times(1)).findById(tripId);
        }

        @Test
        void getPaymentForTrip_shouldReturnCachedResponseOnCacheHit() {
                var first = paymentQueryService.getPaymentForTrip(tripId, riderId);
                var second = paymentQueryService.getPaymentForTrip(tripId, riderId);

                assertEquals(first, second);
                verify(repository, times(1)).findById(tripId);
        }

        @Test
        void getPaymentForTrip_shouldNotShareCacheBetweenDifferentRiders() {
                var legitimateResponse = paymentQueryService.getPaymentForTrip(
                                tripId,
                                riderId);

                assertNotNull(legitimateResponse);

                UUID anotherRiderId = UUID.randomUUID();

                assertThrows(
                                IllegalOperationException.class,
                                () -> paymentQueryService.getPaymentForTrip(
                                                tripId,
                                                anotherRiderId));

                // Both rider requests must independently reach the repository.
                verify(repository, times(2)).findById(tripId);
        }

        @Test
        void getPaymentForTrip_shouldNotCacheUnauthorizedResponse() {
                UUID unauthorizedRiderId = UUID.randomUUID();

                assertThrows(
                                IllegalOperationException.class,
                                () -> paymentQueryService.getPaymentForTrip(
                                                tripId,
                                                unauthorizedRiderId));

                assertThrows(
                                IllegalOperationException.class,
                                () -> paymentQueryService.getPaymentForTrip(
                                                tripId,
                                                unauthorizedRiderId));

                // Failed authorization must not create a cache entry.
                verify(repository, times(2)).findById(tripId);
        }

        @Test
        void createPaymentForTrip_shouldEvictExistingCache() {
                var cached = paymentQueryService.getPaymentForTrip(
                                tripId, riderId);

                assertEquals(PaymentStatus.SUCCESS, cached.status());

                Instant createdAt = Instant.now();

                TripPayment newPayment = new TripPayment(
                                tripId,
                                UUID.randomUUID(),
                                riderId,
                                new BigDecimal("300.00"),
                                "INR",
                                PaymentStatus.PENDING,
                                createdAt);

                when(repository.findById(tripId)).thenReturn(Optional.of(newPayment));

                paymentQueryService.createPaymentForTrip(
                                new PaymentCreatedEvent(
                                                UUID.randomUUID(),
                                                newPayment.getPaymentId(),
                                                tripId,
                                                riderId,
                                                newPayment.getAmount(),
                                                newPayment.getCurrency(),
                                                createdAt));

                var refreshed = paymentQueryService.getPaymentForTrip(
                                tripId, riderId);

                assertEquals(PaymentStatus.PENDING, refreshed.status());
                assertEquals(new BigDecimal("300.00"), refreshed.amount());

                verify(repository, times(2)).findById(tripId);
        }

        @Test
        void projectPaymentSucceeded_shouldEvictExistingCache() {
                TripPayment pendingPayment = new TripPayment(
                                tripId,
                                paymentId,
                                riderId,
                                new BigDecimal("250.00"),
                                "INR",
                                PaymentStatus.PENDING,
                                Instant.now());

                when(repository.findById(tripId))
                                .thenReturn(Optional.of(payment))
                                .thenReturn(Optional.of(pendingPayment));

                paymentQueryService.getPaymentForTrip(tripId, riderId);

                Instant paidAt = Instant.now();

                paymentProjection.project(
                                new PaymentSucceededEvent(
                                                UUID.randomUUID(),
                                                paymentId,
                                                tripId,
                                                riderId,
                                                new BigDecimal("250.00"),
                                                "INR",
                                                paidAt));

                var refreshed = paymentQueryService.getPaymentForTrip(
                                tripId, riderId);

                assertEquals(PaymentStatus.SUCCESS, refreshed.status());
                assertEquals(paidAt, refreshed.paidAt());

                verify(repository, times(3)).findById(tripId);
        }

        @Test
        void projectPaymentFailed_shouldEvictExistingCache() {
                TripPayment pendingPayment = new TripPayment(
                                tripId,
                                paymentId,
                                riderId,
                                new BigDecimal("250.00"),
                                "INR",
                                PaymentStatus.PENDING,
                                Instant.now());

                when(repository.findById(tripId))
                                .thenReturn(Optional.of(payment))
                                .thenReturn(Optional.of(pendingPayment));

                paymentQueryService.getPaymentForTrip(tripId, riderId);

                Instant failedAt = Instant.now();

                paymentProjection.project(
                                new PaymentFailedEvent(
                                                UUID.randomUUID(),
                                                paymentId,
                                                tripId,
                                                riderId,
                                                new BigDecimal("250.00"),
                                                "INR",
                                                failedAt));

                var refreshed = paymentQueryService.getPaymentForTrip(
                                tripId, riderId);

                assertEquals(PaymentStatus.FAILED, refreshed.status());

                verify(repository, times(3)).findById(tripId);
        }

        @Test
        void projectPaymentRefunded_shouldEvictExistingCache() {
                TripPayment successfulPayment = new TripPayment(
                                tripId,
                                paymentId,
                                riderId,
                                new BigDecimal("250.00"),
                                "INR",
                                PaymentStatus.SUCCESS,
                                Instant.now());

                when(repository.findById(tripId))
                                .thenReturn(Optional.of(payment))
                                .thenReturn(Optional.of(successfulPayment));

                paymentQueryService.getPaymentForTrip(tripId, riderId);

                Instant refundedAt = Instant.now();

                paymentProjection.project(
                                new PaymentRefundedEvent(
                                                UUID.randomUUID(),
                                                paymentId,
                                                tripId,
                                                riderId,
                                                new BigDecimal("250.00"),
                                                "INR",
                                                refundedAt));

                var refreshed = paymentQueryService.getPaymentForTrip(
                                tripId, riderId);

                assertEquals(PaymentStatus.REFUNDED, refreshed.status());
                assertEquals(refundedAt, refreshed.refundedAt());

                verify(repository, times(3)).findById(tripId);
        }
}
